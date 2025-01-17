/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.batch.configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This batch infrastructure configuration is quite similar to the
 * {@link org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer}, it only
 * introduces a {@link org.springframework.core.task.TaskExecutor} used in the {@link org.springframework.batch.core.launch.support.SimpleJobLauncher}
 * for starting jobs
 * asynchronously. Its thread pool is configured to four threads by default, which can be changed
 * by setting the property batch.max.pool.size to a different number.
 * The {@link org.springframework.core.task.TaskExecutor} may also be used in job configurations
 * for multi-threaded job execution. In XML you can use it by name, which is taskExecutor. In JavaConfig,
 * you can either autowire {@link org.springframework.core.task.TaskExecutor} or, if you want to know
 * where it's configured, this class.
 * 
 * @author Tobias Flohre
 * 
 */
@ConditionalOnMissingBean(BatchConfigurer.class)
@Configuration
public class TaskExecutorBatchConfigurer implements BatchConfigurer {

	private static final Log logger = LogFactory.getLog(TaskExecutorBatchConfigurer.class);

	@Autowired
	private Environment env;

	private DataSource dataSource;
	private PlatformTransactionManager transactionManager;
	private JobRepository jobRepository;
	private JobLauncher jobLauncher;
	private JobExplorer jobExplorer;

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setMaxPoolSize(Integer.parseInt(env.getProperty("batch.max.pool.size", "4")));
		taskExecutor.afterPropertiesSet();
		return taskExecutor;
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.transactionManager = new DataSourceTransactionManager(dataSource);
	}

	@Override
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	@Override
	public JobLauncher getJobLauncher() {
		return jobLauncher;
	}

	@Override
	public JobExplorer getJobExplorer() throws Exception {
		return jobExplorer;
	}

	private JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(taskExecutor());
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	protected JobRepository createJobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(dataSource);
		factory.setTransactionManager(transactionManager);
		String isolationLevelForCreate = env.getProperty("batch.repository.isolationlevelforcreate");
		if (isolationLevelForCreate != null) {
			factory.setIsolationLevelForCreate(isolationLevelForCreate);
		}
		String tablePrefix = env.getProperty("batch.repository.tableprefix");
		if (tablePrefix != null) {
			factory.setTablePrefix(tablePrefix);
		}
		factory.afterPropertiesSet();
		return (JobRepository) factory.getObject();
	}

	@PostConstruct
	public void initialize() throws Exception {
		if (dataSource == null) {
			logger.warn("No datasource was provided...using a Map based JobRepository");

			if (this.transactionManager == null) {
				this.transactionManager = new ResourcelessTransactionManager();
			}

			MapJobRepositoryFactoryBean jobRepositoryFactory = new MapJobRepositoryFactoryBean(this.transactionManager);
			jobRepositoryFactory.afterPropertiesSet();
			this.jobRepository = jobRepositoryFactory.getObject();

			MapJobExplorerFactoryBean jobExplorerFactory = new MapJobExplorerFactoryBean(jobRepositoryFactory);
			jobExplorerFactory.afterPropertiesSet();
			this.jobExplorer = jobExplorerFactory.getObject();
		} else {
			this.jobRepository = createJobRepository();

			JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
			jobExplorerFactoryBean.setDataSource(this.dataSource);
			jobExplorerFactoryBean.afterPropertiesSet();
			this.jobExplorer = jobExplorerFactoryBean.getObject();
		}

		this.jobLauncher = createJobLauncher();
	}

}
