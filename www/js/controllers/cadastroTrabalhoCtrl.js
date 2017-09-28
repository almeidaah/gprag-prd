gprag.controller('trabalhoCtrl', function(trabalhoService, clienteService, $window, $state, $scope) {

    var ctrl = this;
    ctrl.trabalho={};
    ctrl.listClients={};
    ctrl.msgErro = "";

    //Minimo de 1 ano a frente
    var data = ctrl.trabalho.periodoNecessidade = new Date();
    data.setFullYear(data.getFullYear() +1);
    ctrl.trabalho.periodoNecessidade = data;

    clienteService.findAll()
    .success(function(response){
      ctrl.listClients = response;
      ctrl.trabalho.idCliente = ctrl.listClients[0].id;
    })
    .error(function(error){
      ctrl.msgErro = "Não foi possível buscar a lista de clientes. Tente novamente em alguns minutos.";
    });

    ctrl.trabalho.tipoTrabalho = [
    {
      text: 'DTT',
      enabled: 'false'
    },
    {
      text: 'DCP',
      enabled: 'false'
    },
    {
      text: 'DRT',
      enabled: 'false'
    },
    {
      text: 'LCA',
      enabled: 'false'
    }];

    ctrl.cadastrarTrabalho = function(trabalho){
      trabalhoService.cadastrar(trabalho)
      .success(function(response){
        $state.go("sucesso");
      })
      .error(function(error){
        console.error(error);
      })
    };

    ctrl.voltar = function(){
      $state.go("home");
    }
});
