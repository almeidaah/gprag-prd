gprag.controller('avisosCtrl', function($window, $state, $rootScope, avisosService) {

    var ctrl = this;

    ctrl.msgErro = "";
    ctrl.listAVencer={};
    ctrl.listVencidos ={};

    ctrl.voltar = function(){
      $state.go("home");
  	}

    avisosService.findClientsToExpire()
    .success(function(response){
    ctrl.listAVencer = response;

    var actualDate = new Date();
    actualDate.setHours(0,0,0,0);

      angular.forEach(ctrl.listAVencer, function(cliente){
        cliente.venceEm = daydiff(cliente.periodoNecessidade, actualDate);
       });

       ctrl.listAVencer.sort(function (a, b){
         return a.venceEm - b.venceEm;
       });

       angular.forEach(ctrl.listAVencer, function(cliente){
         cliente.venceEm = (cliente.venceEm == 0) ? 'Hoje' : cliente.venceEm + ' dia(s)';
        });

    })
    .error(function(error){
      ctrl.msgErro = "Serviço indisponível no momento.";
    });

    avisosService.findExpiredClients()
    .success(function(response){
    ctrl.listVencidos = response;

    var actualDate = new Date();
    actualDate.setHours(0,0,0,0);

      angular.forEach(ctrl.listVencidos, function(cliente){
        cliente.diasVencido = daydiff(actualDate, cliente.periodoNecessidade);
       });

       ctrl.listVencidos.sort(function (a, b){
         return a.diasVencido - b.diasVencido;
       });

       angular.forEach(ctrl.listVencidos, function(cliente){
         cliente.diasVencido = (cliente.diasVencido == 0) ? 'Hoje' : cliente.diasVencido + ' dia(s)';
        });
    });

    ctrl.editarCliente = function(clienteEdit){
      $rootScope.clienteEdit = clienteEdit;
      $state.go("cadastro/cliente");
    };

})
