// Ionic Starter App

// angular.module is a global place for creating, registering and retrieving Angular modules
// 'starter' is the name of this angular module example (also set in a <body> attribute in index.html)
// the 2nd parameter is an array of 'requires'
var gprag = angular.module('starter', ['ionic', 'gpragDirectives', 'ngCordova'])

.run(function($ionicPlatform) {
  $ionicPlatform.ready(function() {

    if(window.StatusBar) {
      StatusBar.styleDefault();
    }
  });
})

.constant('SERVICE_URL','http://gprag-service.herokuapp.com')
//.constant('SERVICE_URL','http://192.168.1.110:8080')
//.constant('SERVICE_URL','http://localhost:8080')

.config(function($stateProvider, $urlRouterProvider) {

  $urlRouterProvider.otherwise('/home');

  $stateProvider
    .state('home', {
      url: '/home',
      cache : false,
      templateUrl: 'templates/home.html',
      controller: 'homeCtrl',
      controllerAs: 'ctrl'
    })
    .state('cadastro/cliente', {
      url: '/cadastro/cliente',
      cache : false,
      templateUrl: 'templates/cadastro-cliente.html',
      controller: 'clienteCtrl',
      controllerAs: 'ctrl'
    })
    .state('cadastro/trabalho', {
      url: '/cadastro/trabalho',
      cache : false,
      templateUrl: 'templates/cadastro-trabalho.html',
      controller: 'trabalhoCtrl',
      controllerAs: 'ctrl'
    })
    .state('buscar/cliente', {
      url: '/buscar/cliente',
      cache: false,
      templateUrl: 'templates/buscar-cliente.html',
      controller: 'buscarClienteCtrl',
      controllerAs: 'ctrl'
    })
    .state('cliente/trabalhos', {
      url: '/cliente/trabalhos',
      cache: false,
      templateUrl: 'templates/list-trabalhos.html',
      controller: 'buscarClienteCtrl',
      controllerAs: 'ctrl'
    })
    .state('avisos', {
      url: '/avisos',
      cache: false,
      templateUrl: 'templates/avisos.html',
      controller: 'avisosCtrl',
      controllerAs: 'ctrl'
    })
    // Estado default de sucesso
    .state('sucesso', {
      url: '/sucesso',
      templateUrl: 'templates/sucesso.html',
      controller: 'clienteCtrl',
      controllerAs: 'ctrl'
    });
});
