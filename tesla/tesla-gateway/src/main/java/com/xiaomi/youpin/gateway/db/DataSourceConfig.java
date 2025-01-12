/*
 *  Copyright 2020 Xiaomi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.xiaomi.youpin.gateway.db;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.xiaomi.youpin.gateway.config.DaoAuthConfig;
import com.xiaomi.youpin.gateway.db.aop.DaoAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Dao;
import org.nutz.dao.impl.NutDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;

@Slf4j
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.driverClassName}")
    private String driverClass;

    @Value("${spring.datasource.default.initialPoolSize}")
    private Integer defaultInitialPoolSize;

    @Value("${spring.datasource.default.maxPoolSize}")
    private Integer defaultMaxPoolSize;

    @Value("${datasource.isinit}")
    private String isNeedInit;

    @Value("${datasource.isNeedAuthCheck:false}")
    private String isNeedAuthCheck;

    @Value("${spring.datasource.default.minialPoolSize}")
    private Integer defaultMinPoolSize;

    @NacosValue(value = "${spring.datasource.username}", autoRefreshed = true)
    private String dataSourceUserName;

    @NacosValue(value = "${spring.datasource.url}", autoRefreshed = true)
    private String dataSourceUrl;

    @NacosValue(value = "${spring.datasource.password}", autoRefreshed = true)
    private String dataSourcePasswd;

    @Bean(name = "masterDataSource")
    @Primary
    public DataSource masterDataSource() throws PropertyVetoException, NamingException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass(driverClass);
        dataSource.setJdbcUrl(dataSourceUrl);
        dataSource.setUser(dataSourceUserName);
        dataSource.setPassword(dataSourcePasswd);
        dataSource.setInitialPoolSize(defaultInitialPoolSize);
        dataSource.setMaxPoolSize(defaultMaxPoolSize);
        dataSource.setMinPoolSize(defaultMinPoolSize);
        setDatasouce(dataSource);
        return dataSource;
    }

    private void setDatasouce(ComboPooledDataSource dataSource) {
        dataSource.setTestConnectionOnCheckin(true);
        dataSource.setTestConnectionOnCheckout(false);
        dataSource.setPreferredTestQuery("select 1");
        dataSource.setIdleConnectionTestPeriod(180);
    }


    /**
     * 提供给script filter 数据库操作的能力
     *
     * @param masterDataSource
     * @return
     */
    @Bean
    public Dao dao(@Qualifier("masterDataSource") DataSource masterDataSource, DaoAuthConfig daoAuthConfig) {
        if ("true".equals(isNeedInit)) {
            log.info("init dao");
            NutDao dao = new NutDao(masterDataSource);

            // 开启权限校验
            if ("true".equals(isNeedAuthCheck)) {
                DaoAuthInterceptor authInterceptor = new DaoAuthInterceptor(daoAuthConfig);
                dao.addInterceptor(authInterceptor);
            }

            return dao;
        } else {
            return new NutDao();
        }
    }
}
