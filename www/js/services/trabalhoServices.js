gprag.factory('trabalhoService', function ($http, SERVICE_URL) {

  $http.defaults.headers.common = {
      'Authorization': 'Basic Z3ByYWctc2VydmljZTpncHJhZy1zZXJ2aWNl',
      'Accept': 'application/json;odata=verbose',
      'Access-Control-Allow-Origin':'*'
  };

  var _cadastrar = function (trabalho) {

    return $http({
          url: SERVICE_URL + '/gprag/v1/job',
          method: 'POST',
          params: {
            'idCliente' : trabalho.idCliente,
            'tipoTrabalho' : JSON.stringify(trabalho.tipoTrabalho),
            'valorTotal' : trabalho.valorTotal,
            'representante' : trabalho.representante,
            'aplicador' : trabalho.aplicador,
            'periodoNecessidade': trabalho.periodoNecessidade.getTime()
          }
      });
  };

  var _findByIdCliente = function(idCliente){

    return $http({
      //url: 'http://localhost.168.13.67:8080' + '/gprag/v1/job/client/' + idCliente,
      url: SERVICE_URL + '/gprag/v1/job/client/' + idCliente,
      method: 'GET'
    });
  }

  var _removerTrabalho = function (id) {
      return $http({
          url: SERVICE_URL + '/gprag/v1/job/' + id,
          method: 'DELETE',
      });
  };

  return{
    cadastrar : _cadastrar,
    findByIdCliente : _findByIdCliente,
    removerTrabalho : _removerTrabalho
  }

});
