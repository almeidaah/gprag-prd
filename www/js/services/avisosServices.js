gprag.factory('avisosService', function ($http, SERVICE_URL) {

  $http.defaults.headers.common = {
      'Accept': 'application/json;odata=verbose'
  };

  var _findClientsToExpire = function (cliente) {
      return $http({
          url: SERVICE_URL + '/gprag/v1/alerts/findClientsToExpire',
          method: 'GET'
      });
  };

  var _findExpiredClients = function (cliente) {
      return $http({
          url: SERVICE_URL + '/gprag/v1/alerts/findExpiredClients',
          method: 'GET'
      });
  };

  return{
    findClientsToExpire :_findClientsToExpire,
    findExpiredClients : _findExpiredClients
  }
});
