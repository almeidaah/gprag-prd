gprag.controller('homeCtrl', function($window, $state, $scope, $cordovaLocalNotification, avisosService) {

    var ctrl = this;
    $scope.semConexao = true;
    $scope.mensagemErro = "Sem conexão com a internet";

    ctrl.erroConexao = false;
    ctrl.mensagemErro = "";

    avisosService.findClientsToExpire()
    .success(function(response){
      ctrl.listAVencer = response;

      if(ctrl.listAVencer.length > 0){
        $cordovaLocalNotification.add({
            id: "1",
            date: new Date(),
            message: "Você possui " + ctrl.listAVencer.length + " clientes a vencer!",
            title: "ATENÇÃO!"
        }).then(function () {
            console.log("The notification has been set");
        });
      }
    });

    ctrl.listarAvisos = function(){
      $state.go("avisos");
    }

    ctrl.novoCadastroCliente = function(){
      $state.go("cadastro/cliente");
    }

    ctrl.novoCadastroTrabalho = function(){
      $state.go("cadastro/trabalho");
    }

    ctrl.buscarCliente = function(){
      $state.go("buscar/cliente");
    }

})
