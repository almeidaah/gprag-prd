gprag.factory('clienteService', function ($http, SERVICE_URL) {

  $http.defaults.headers.common = {
      'Accept': 'application/json;odata=verbose'
  };

  var _cadastrar = function (cliente) {

      var periodoNecessidade = new Date(cliente.periodoNecessidade);
      periodoNecessidade.setHours(0,0,10,0);

      return $http({
          url: SERVICE_URL + '/gprag/v1/clients',
          method: 'POST',
          params: {
            'id' : cliente.id,
            'nome' : cliente.nome,
            'email': cliente.email,
            'endereco': cliente.endereco,
            'periodoNecessidade': periodoNecessidade.getTime(),
            'status': cliente.status,
            'telefone': cliente.telefone
          }
      });
  };

  var _findAll = function () {
      return $http({
          url: SERVICE_URL + '/gprag/v1/clients',
          method: 'GET',
      });
  };

  var _findByName = function (nome) {
      return $http({
          url: SERVICE_URL + '/gprag/v1/clients/' + nome,
          method: 'GET',
      });
  };

  var _removerCliente = function (id) {
      return $http({
          url: SERVICE_URL + '/gprag/v1/clients/' + id,
          method: 'DELETE',
      });
  };

  var _atualizarPeriodoCliente = function (clienteId, meses) {
      return $http({
          url: SERVICE_URL + '/gprag/v1/clients/atualizarPeriodoCliente/' + clienteId,
          params :{
            'meses' : meses
          },
          method: 'POST',
      });
  };

  return{
    cadastrar : _cadastrar,
    findAll : _findAll,
    findByName : _findByName,
    removerCliente: _removerCliente,
    atualizarPeriodoCliente : _atualizarPeriodoCliente
  }
});
