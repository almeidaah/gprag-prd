var gpragDirectives = angular.module("gpragDirectives", []);

gpragDirectives.directive("checkInternet", function($interval){

    return {
        restrict: "E",
        controller: function ($scope, $element, $attrs, $interval) {
            $interval(function () {
                if (navigator.network.connection.type === Connection.NONE) {
                    $scope.semConexao = true;
                    $scope.mensagemErro = "Sem conexão com a internet";
                    return;
                }
                $scope.semConexao = false;
            }, 1000);
        },
        templateUrl: "js/partials/erro-conexao.html"
    };

})
