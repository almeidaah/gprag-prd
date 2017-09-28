gprag.controller('buscarClienteCtrl', function($window, $state, $rootScope, $ionicPopup, clienteService, trabalhoService ) {

    var ctrl = this;
    ctrl.nmCliente = "";
    ctrl.msgErro = "";
    ctrl.msgSucesso = "";
    ctrl.listtrabalhos = "";

    /*Seta foco no campo de busca ao abrir a tela*/
    //Removido porquê atrapalhava ao voltar de uma edição
    // angular.element(document).ready(function () {
    //     document.getElementById('nomeCliente').focus();
    // });

    ctrl.listTrabalhos = function(){
      ctrl.listTrabalhos = JSON.parse(sessionStorage.getItem('listTrabalhos'));
      angular.forEach(ctrl.listTrabalhos, function(trabalho){
        var data = new Date(trabalho.periodoNecessidade);
          trabalho.tipoTrabalho = JSON.parse(trabalho.tipoTrabalho);
          trabalho.periodoNecessidade = data.getDate() + '/' + (data.getMonth()+1) + '/' + data.getFullYear();
       });
    }

    findAllClients();

    function findAllClients(){
      ctrl.listClients = clienteService.findAll()
      .success(function(response){
        ctrl.listClients = response;
      })
      .error(function(error){
        ctrl.msgErro = "Não foi possível buscar a lista de clientes. Tente novamente em alguns minutos.";
      })
    }

    ctrl.voltar = function(){
      $state.go("home");
    }

    ctrl.buscarCliente = function(){

      if(!ctrl.nmCliente ){
        findAllClients();
      }else{
        if(ctrl.nmCliente.length >= 3){
          clienteService.findByName(ctrl.nmCliente)
          .success(function(response){
            ctrl.listClients = response;
            ctrl.msgErro="";
            ctrl.msgSucesso="";
          })
          .error(function(error){
            ctrl.msgErro = "O serviço está indisponível no momento, tente em alguns minutos ou informe o administrador do sistema.";
          });
        }
     }
    };

    ctrl.editarCliente = function(clienteEdit){
      $rootScope.clienteEdit = clienteEdit;
      $state.go("cadastro/cliente");
    };

    ctrl.removerCliente = function(clienteId){
            $ionicPopup.confirm({
             title: 'ATENÇÃO!',
             template: 'Deseja remover este cliente?' +
             '<br/><h4><b>(TODOS os trabalhos do cliente serão removidos!)</b>'
           }).then(function(res) {
            if (res) {
              clienteService.removerCliente(clienteId)
              .success(function(response){
                ctrl.msgSucesso = "Cliente removido com sucesso";
                ctrl.nmCliente = "";
                ctrl.listClients = {};
                findAllClients();
              })
              .error(function(error){
                console.error(error);
              });
            }
          });
    };

    ctrl.removerTrabalho = function(trabalhoId){
            $ionicPopup.confirm({
             title: 'ATENÇÃO!',
             template: 'Deseja remover este trabalho?'
           }).then(function(res) {
            if (res) {
              trabalhoService.removerTrabalho(trabalhoId)
              .success(function(response){
                ctrl.msgSucesso = "trabalho removido com sucesso";
                ctrl.nmCliente = "";
                ctrl.listTrabalhos = {};
              })
              .error(function(error){
                console.error(error);
              });
            }
          });
    };

    ctrl.buscarTrabalhos = function(idCliente){
        trabalhoService.findByIdCliente(idCliente)
        .success(function(response){
          sessionStorage.setItem('listTrabalhos', JSON.stringify(response));
          $state.go("cliente/trabalhos", {reload:false});
        })
        .error(function(error){
          console.error(error);
        });
      };

      ctrl.voltarBusca = function(){
        $state.transitionTo("buscar/cliente", {reload: false} );
      }

})
