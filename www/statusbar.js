/*
 *
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

/* global cordova */

var exec = require('cordova/exec');

var namedColors = {
    black: '#000000',
    darkGray: '#A9A9A9',
    lightGray: '#D3D3D3',
    white: '#FFFFFF',
    gray: '#808080',
    red: '#FF0000',
    green: '#00FF00',
    blue: '#0000FF',
    cyan: '#00FFFF',
    yellow: '#FFFF00',
    magenta: '#FF00FF',
    orange: '#FFA500',
    purple: '#800080',
    brown: '#A52A2A'
};

/**
 * Helper function to execute cordova commands
 * @param {string} action - The action to execute
 * @param {Array} args - Arguments for the action
 * @param {Function} successCallback - Optional success callback
 * @param {Function} errorCallback - Optional error callback
 * @returns {*} - Return value from success callback if provided
 */
function execStatusBarCommand(action, args, successCallback, errorCallback) {
    return exec(
        function(result) {
            if (typeof successCallback === 'function') {
                return successCallback(result);
            }
            return result;
        },
        function(error) {
            if (typeof errorCallback === 'function') {
                errorCallback(error);
            }
        },
        'StatusBar',
        action,
        args || []
    );
}

/**
 * Simple command execution without callbacks
 * @param {string} action - The action to execute
 * @param {Array} args - Arguments for the action
 */
function execSimpleCommand(action, args) {
    exec(null, null, 'StatusBar', action, args || []);
}

var StatusBar = {
    isVisible: true,

    overlaysWebView: function(doOverlay) {
        execSimpleCommand('overlaysWebView', [doOverlay]);
    },

    styleDefault: function() {
        // dark text (to be used on a light background)
        execSimpleCommand('styleDefault');
    },

    styleLightContent: function() {
        // light text (to be used on a dark background)
        execSimpleCommand('styleLightContent');
    },

    backgroundColorByName: function(colorname) {
        return StatusBar.backgroundColorByHexString(namedColors[colorname]);
    },

    backgroundColorByHexString: function(hexString) {
        if (hexString.charAt(0) !== '#') {
            hexString = '#' + hexString;
        }

        if (hexString.length === 4) {
            var split = hexString.split('');
            hexString = '#' + split[1] + split[1] + split[2] + split[2] + split[3] + split[3];
        }

        execSimpleCommand('backgroundColorByHexString', [hexString]);
    },

    hide: function() {
        execSimpleCommand('hide');
        StatusBar.isVisible = false;
    },

    show: function() {
        execSimpleCommand('show');
        StatusBar.isVisible = true;
    },

    /**
     * Gets the height of various UI elements
     * @param {string} elementType - The element type to get height for
     * @param {Function} successCallback - Success callback
     * @param {Function} errorCallback - Error callback
     * @returns {*} - Return value from success callback
     */
    getHeight: function(elementType, successCallback, errorCallback) {
        return execStatusBarCommand(elementType, [], successCallback, errorCallback);
    },

    getNavigationBarHeight: function(successCallback, errorCallback) {
        return StatusBar.getHeight('getNavigationBarHeight', successCallback, errorCallback);
    },

    getStatusBarHeight: function(successCallback, errorCallback) {
        return StatusBar.getHeight('getStatusBarHeight', successCallback, errorCallback);
    },

    getTotalScreenHeight: function(successCallback, errorCallback) {
        return StatusBar.getHeight('getTotalScreenHeight', successCallback, errorCallback);
    }
};

// prime it. setTimeout so that proxy gets time to init
window.setTimeout(function() {
    exec(
        function(res) {
            if (typeof res === 'object') {
                if (res.type === 'tap') {
                    cordova.fireWindowEvent('statusTap');
                }
            } else {
                StatusBar.isVisible = res;
            }
        },
        null,
        'StatusBar',
        '_ready',
        []
    );
}, 0);

module.exports = StatusBar;
