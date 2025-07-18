/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

function notSupported (win, fail) {
    //
    console.log('StatusBar is not supported');
    setTimeout(function () {
        if (win) {
            win();
        }
        // note that while it is not explicitly supported, it does not fail
        // this is really just here to allow developers to test their code in the browser
        // and if we fail, then their app might as well. -jm
    }, 0);
}

module.exports = {
    isVisible: false,
    styleDefault: notSupported,
    styleLightContent: notSupported,
    overlaysWebView: notSupported,
    backgroundColorByName: notSupported,
    backgroundColorByHexString: notSupported,
    hide: notSupported,
    show: notSupported,
    _ready: notSupported,
    getNavigationBarHeight: notSupported,
    getStatusBarHeight: notSupported,
    getTotalScreenHeight: notSupported
};

require('cordova/exec/proxy').add('StatusBar', module.exports);
