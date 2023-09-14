'use strict';

/**
 * @ngdoc overview
 * @name sentinelDashboardApp
 * @description
 * # sentinelDashboardApp
 *
 * Main module of the application.
 */

angular
  .module('sentinelDashboardApp', [
    'oc.lazyLoad',
    'ui.router',
    'ui.bootstrap',
    'angular-loading-bar',
    'ngDialog',
    'ui.bootstrap.datetimepicker',
    'ui-notification',
    'rzTable',
    'angular-clipboard',
    'selectize',
    'angularUtils.directives.dirPagination'
  ])
  .factory('AuthInterceptor', ['$window', '$state', function ($window, $state) {
    var authInterceptor = {
      'responseError' : function(response) {
        if (response.status === 401) {
          // If not auth, clear session in localStorage and jump to the login page
          $window.localStorage.removeItem('session_sentinel_admin');
          $state.go('login');
        }

        return response;
      },
      'response' : function(response) {
        return response;
      },
      'request' : function(config) {
        // Resolved resource loading failure after configuring ContextPath
    	  var baseUrl = $window.document.getElementsByTagName('base')[0].href;
    	  config.url = baseUrl + config.url;
        return config;
      },
      'requestError' : function(config){
        return config;
      }
    };
    return authInterceptor;
  }])
  .config(['$stateProvider', '$urlRouterProvider', '$ocLazyLoadProvider', '$httpProvider',
    function ($stateProvider, $urlRouterProvider, $ocLazyLoadProvider, $httpProvider) {
      $httpProvider.interceptors.push('AuthInterceptor');

      $ocLazyLoadProvider.config({
        debug: false,
        events: true,
      });

      $urlRouterProvider.otherwise('/dashboard/home');

      $stateProvider
        .state('login', {
            url: '/login',
            templateUrl: 'app/views/login.html',
            controller: 'LoginCtl',
            resolve: {
                loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                    return $ocLazyLoad.load({
                        name: 'sentinelDashboardApp',
                        files: [
                            'app/scripts/controllers/login.js',
                        ]
                    });
                }]
            }
        })

      .state('dashboard', {
        url: '/dashboard',
        templateUrl: 'app/views/dashboard/main.html',
        resolve: {
          loadMyDirectives: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load(
              {
                name: 'sentinelDashboardApp',
                files: [
                  'app/scripts/directives/header/header.js',
                  'app/scripts/directives/sidebar/sidebar.js',
                  'app/scripts/directives/sidebar/sidebar-search/sidebar-search.js',
                ]
              });
          }]
        }
      })

      .state('dashboard.home', {
        url: '/home',
        templateUrl: 'app/views/dashboard/home.html',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/main.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.machine', {
        templateUrl: 'app/views/machine.html',
        url: '/app/:app',
        controller: 'MachineCtl',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/machine.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.metric', {
        templateUrl: 'app/views/metric.html',
        url: '/metric/:app',
        controller: 'MetricCtl',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/metric.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.clusterAppAssignManage', {
          templateUrl: 'app/views/cluster_app_assign_manage.html',
          url: '/cluster/assign_manage/:app',
          controller: 'SentinelClusterAppAssignManageController',
          resolve: {
              loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                  return $ocLazyLoad.load({
                      name: 'sentinelDashboardApp',
                      files: [
                          'app/scripts/controllers/cluster_app_assign_manage.js',
                      ]
                  });
              }]
          }
      })

      .state('dashboard.clusterAppServerList', {
          templateUrl: 'app/views/cluster_app_server_list.html',
          url: '/cluster/server/:app',
          controller: 'SentinelClusterAppServerListController',
          resolve: {
              loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                  return $ocLazyLoad.load({
                      name: 'sentinelDashboardApp',
                      files: [
                          'app/scripts/controllers/cluster_app_server_list.js',
                      ]
                  });
              }]
          }
      })

      .state('dashboard.clusterAppClientList', {
          templateUrl: 'app/views/cluster_app_client_list.html',
          url: '/cluster/client/:app',
          controller: 'SentinelClusterAppTokenClientListController',
          resolve: {
              loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                  return $ocLazyLoad.load({
                      name: 'sentinelDashboardApp',
                      files: [
                          'app/scripts/controllers/cluster_app_token_client_list.js',
                      ]
                  });
              }]
          }
      })

      .state('dashboard.clusterSingle', {
          templateUrl: 'app/views/cluster_single_config.html',
          url: '/cluster/single/:app',
          controller: 'SentinelClusterSingleController',
          resolve: {
              loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                  return $ocLazyLoad.load({
                      name: 'sentinelDashboardApp',
                      files: [
                          'app/scripts/controllers/cluster_single.js',
                      ]
                  });
              }]
          }
      })

      .state('dashboard.identityV1', {
        templateUrl: 'app/views/v1/identity.html',
        url: '/v1/identity/:app',
        controller: 'IdentityCtlV1',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v1/identity.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.identityV2', {
        templateUrl: 'app/views/v2/identity.html',
        url: '/v2/identity/:app',
        controller: 'IdentityCtlV2',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v2/identity.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.flowV1', {
        templateUrl: 'app/views/v1/rule/flow.html',
        url: '/v1/rule/flow/:app',
        controller: 'FlowControllerV1',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v1/rule/flow.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.flowV2', {
          templateUrl: 'app/views/v2/rule/flow.html',
          url: '/v2/rule/flow/:app',
          controller: 'FlowControllerV2',
          resolve: {
              loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                  return $ocLazyLoad.load({
                      name: 'sentinelDashboardApp',
                      files: [
                          'app/scripts/controllers/v2/rule/flow.js',
                      ]
                  });
              }]
          }
      })

      .state('dashboard.paramFlowV1', {
        templateUrl: 'app/views/v1/rule/param_flow.html',
        url: '/v1/rule/paramFlow/:app',
        controller: 'ParamFlowControllerV1',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v1/rule/param_flow.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.paramFlowV2', {
        templateUrl: 'app/views/v2/rule/param_flow.html',
        url: '/v2/rule/paramFlow/:app',
        controller: 'ParamFlowControllerV2',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v2/rule/param_flow.js',
              ]
            });
          }]
        }
      })
      .state('dashboard.authorityV1', {
            templateUrl: 'app/views/v1/rule/authority.html',
            url: '/v1/rule/authority/:app',
            controller: 'AuthorityRuleControllerV1',
            resolve: {
                loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                    return $ocLazyLoad.load({
                        name: 'sentinelDashboardApp',
                        files: [
                            'app/scripts/controllers/v1/rule/authority.js',
                        ]
                    });
                }]
            }
       })

      .state('dashboard.authorityV2', {
            templateUrl: 'app/views/v2/rule/authority.html',
            url: '/v2/rule/authority/:app',
            controller: 'AuthorityRuleControllerV2',
            resolve: {
                loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                    return $ocLazyLoad.load({
                        name: 'sentinelDashboardApp',
                        files: [
                            'app/scripts/controllers/v2/rule/authority.js',
                        ]
                    });
                }]
            }
       })

      .state('dashboard.degradeV1', {
        templateUrl: 'app/views/v1/rule/degrade.html',
        url: '/v1/rule/degrade/:app',
        controller: 'DegradeControllerV1',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v1/rule/degrade.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.degradeV2', {
        templateUrl: 'app/views/v2/rule/degrade.html',
        url: '/v2/rule/degrade/:app',
        controller: 'DegradeControllerV2',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v2/rule/degrade.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.systemV1', {
        templateUrl: 'app/views/v1/rule/system.html',
        url: '/v1/rule/system/:app',
        controller: 'SystemCtlV1',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v1/rule/system.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.systemV2', {
        templateUrl: 'app/views/v2/rule/system.html',
        url: '/v2/rule/system/:app',
        controller: 'SystemCtlV2',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v2/rule/system.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.gatewayIdentityV1', {
        templateUrl: 'app/views/v1/gateway_identity.html',
        url: '/v1/gateway/identity/:app',
        controller: 'GatewayIdentityCtlV1',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v1/gateway/identity.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.gatewayIdentityV2', {
        templateUrl: 'app/views/v2/gateway_identity.html',
        url: '/v2/gateway/identity/:app',
        controller: 'GatewayIdentityCtlV2',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v2/gateway/identity.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.gatewayApiV1', {
        templateUrl: 'app/views/v1/gateway/api.html',
        url: '/v1/gateway/api/:app',
        controller: 'GatewayApiCtlV1',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v1/gateway/api.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.gatewayApiV2', {
        templateUrl: 'app/views/v2/gateway/api.html',
        url: '/v2/gateway/api/:app',
        controller: 'GatewayApiCtlV2',
        resolve: {
          loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
            return $ocLazyLoad.load({
              name: 'sentinelDashboardApp',
              files: [
                'app/scripts/controllers/v2/gateway/api.js',
              ]
            });
          }]
        }
      })

      .state('dashboard.gatewayFlowV1', {
          templateUrl: 'app/views/v1/gateway/flow.html',
          url: '/v1/gateway/flow/:app',
          controller: 'GatewayFlowCtlV1',
          resolve: {
              loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                  return $ocLazyLoad.load({
                      name: 'sentinelDashboardApp',
                      files: [
                          'app/scripts/controllers/v1/gateway/flow.js',
                      ]
                  });
              }]
          }
      })

      .state('dashboard.gatewayFlowV2', {
          templateUrl: 'app/views/v2/gateway/flow.html',
          url: '/v2/gateway/flow/:app',
          controller: 'GatewayFlowCtlV2',
          resolve: {
              loadMyFiles: ['$ocLazyLoad', function ($ocLazyLoad) {
                  return $ocLazyLoad.load({
                      name: 'sentinelDashboardApp',
                      files: [
                          'app/scripts/controllers/v2/gateway/flow.js',
                      ]
                  });
              }]
          }
      })
      ;
  }]);