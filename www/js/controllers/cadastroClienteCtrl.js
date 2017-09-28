gprag.controller('clienteCtrl', function(clienteService, $window, $state, $rootScope) {

    var ctrl = this;

    ctrl.cliente = {};
    ctrl.updateCliente = false;

    var clienteEdit = $rootScope.clienteEdit;
    if(clienteEdit){
        ctrl.cliente = $rootScope.clienteEdit;
        $rootScope.clienteEdit = "";
        ctrl.updateCliente = true;
        ctrl.cliente.periodoNecessidade = new Date(clienteEdit.periodoNecessidade);
    }else{
      //Minimo de 1 ano a frente
      var data = ctrl.cliente.periodoNecessidade = new Date();
      data.setFullYear(data.getFullYear() +1);
      ctrl.cliente.periodoNecessidade = data;
    }

    ctrl.cadastrarCliente = function(cliente){
      clienteService.cadastrar(cliente)
      .success(function(response){
        $state.go("sucesso");
      })
      .error(function(error){
        console.error(error);
      });
    };

    ctrl.atualizarPeriodoCliente = function(clienteid, mesesAtualizacao){
      clienteService.atualizarPeriodoCliente(clienteid,mesesAtualizacao)
      .success(function(response){
        ctrl.atualizacaoSucceso = response;
        $state.go("sucesso");
      })
      .error(function(error){
        console.error(error);
      });
    };

    ctrl.voltar = function(){
      $state.go("home");
  	};
})
