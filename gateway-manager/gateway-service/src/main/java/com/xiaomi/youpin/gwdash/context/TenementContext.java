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

package com.xiaomi.youpin.gwdash.context;

import com.xiaomi.youpin.gwdash.bo.SessionAccount;

/**
 * @author goodjava@qq.com
 */
public class TenementContext {

    private static ThreadLocal<TenementContext> context = new ThreadLocal<TenementContext>() {
        @Override
        protected TenementContext initialValue() {
            return new TenementContext();
        }
    };

    private SessionAccount sessionAccount;


    public void set(SessionAccount sessionAccount) {
        this.sessionAccount = sessionAccount;
    }

    public SessionAccount get() {
        return this.sessionAccount;
    }


    public static TenementContext getContext() {
        return context.get();
    }

    public void close() {
        context.remove();
    }


}
