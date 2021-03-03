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

package com.xiaomi.youpin.tesla.agent.po;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author goodjava@qq.com
 * 容器信息
 */
@Data
@Builder
public class ContainerInfo implements Serializable {

    /**
     * 内存大小
     */
    private long mem;

    /**
     * 磁盘权重
     */
    private int blkioWeight;

    /**
     * 使用那些cpu
     */
    private List<Integer> cpus;

    /**
     * 暴露的端口号
     */
    private List<Integer> ports;


    private String buildFile;


}