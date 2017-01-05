/*
 * Copyright 2017. 01. 05. Olivér Falvai
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

package com.ofalvai.bpinfo.api;

import com.ofalvai.bpinfo.model.Alert;

import java.util.List;

public class AlertListMessage {

    public List<Alert> todayAlerts;

    public List<Alert> futureAlerts;

    public AlertListMessage(List<Alert> todayAlerts, List<Alert> futureAlerts) {
        this.todayAlerts = todayAlerts;
        this.futureAlerts = futureAlerts;
    }
}
