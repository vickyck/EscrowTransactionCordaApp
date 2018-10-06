

var app = angular.module('invReportingAppModule', ['ui.router', 'ui.bootstrap', 'ngLoadingOverlay']);

// Fix for unhandled rejections bug.
app.config(['$stateProvider', '$urlRouterProvider', function ($stateProvider, $urlRouterProvider) {
    //$urlRouterProvider.otherwise('/signin');

    $stateProvider
      .state('signin', {
          url: '/signin',
          templateUrl: '/web/bank/Login.html'
      })
      .state('dashboard', {
          url: '/dashboard',
          templateUrl: '/web/bank/Home.html'
      });
}]);
app.controller("DefController", function ($location) {
    $location.path('/signin')
});
app.factory('JSONDataService', ['$http', function ($http) {

    //return {
    var getData = function () {
        return $http.get('/web/bank/Data/metadata.json');
        //   .then(function (response) {
        //    //alert(JSON.stringify(resp));
        //    //return response;
        //    callback(response)
        //},
        //function (error) {
        //    alert(error);
        //});
    }
    return {
        getData: getData
    };
    //};
}]);
app.factory('LoginService', ['$http', 'JSONDataService', function ($http, JSONDataService) {
    var isAuthenticated = false;
    var userSession = {
        loggedInUserName: '',
        isAuthenticated: false
    }
    return {
        login: function () {
            return JSONDataService.getData();
        },
        getLoggedInUserName: function () {
            return userSession.loggedInUserName;
        },
        setLoggedinUserName: function (uname) {
            userSession.loggedInUserName = uname;
        }
    };
}]);
app.controller('HomeController', function ($http, $location, $uibModal, $scope, $window, JSONDataService, LoginService) {
    const demoApp = this;
    $scope.username = LoginService.getLoggedInUserName();
    // We identify the node.
    const apiBaseURL = "http://localhost:10007/api/invreporting/"; // TODO remove harcoded urls
    let peers = [];
    $('#spnToggle').on('click', function () {
        $('.panel-collapse').collapse('toggle');
    });
    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.getEscrows = () => $http.get(apiBaseURL + "escrow-all")
       .then((response) => demoApp.myescrows = Object.keys(response.data)
           .map((key) => response.data[key].state.data)
           .reverse());

    demoApp.getServicings = () => $http.get(apiBaseURL + "servicing-all")
       .then((response) => demoApp.myServicings = Object.keys(response.data)
           .map((key) => response.data[key].state.data)
           .reverse());

    demoApp.getBankBalance = () => $http.get(apiBaseURL + "bank-balance")
           .then((result) => {
               //alert(JSON.stringify(result.data.state.data));
               demoApp.balance = result.data.bankBalanceValue;
           });

    demoApp.getTransactions = () => $http.get(apiBaseURL + "transactions")
        .then((result) => {
            demoApp.trans = result.data;
            //alert(JSON.stringify(demoApp.trans));
        });
    demoApp.receivePayment = function () {
        var url = `${apiBaseURL}receive-payment-post?amount=${$scope.amount}`;
        $http.put(url).then(
               (result) => {
                   setAlertTimeout();
                   regcloseAlert();
                   //alert(result.data);
                   $("#result").html('<div class="alert alert-success"><button type="button" class="close">×</button>' + result.data + '</div>');
                   demoApp.getEscrows();
                   demoApp.getTransactions();
                   demoApp.getBankBalance();
                   $scope.amount = '';
               },
               (result) => {
                   setAlertTimeout();
                   regcloseAlert();
                   $("#result").html('<div class="alert alert-danger"><button type="button" class="close">×</button>' + result.data + '</div>');
                   $scope.amount = '';
               }
           );
    };
    demoApp.setDefaultEscrowScope = function () {
        JSONDataService.getData().then(function (response) {
            //alert(JSON.stringify(response));
            var escrow = response.data.bankdata[0].escrow;
            demoApp.tax = escrow.defaultTax;
            demoApp.insurance = escrow.defaultInsurance;
            $scope.principal = 0;
            //alert(JSON.stringify(escrow));
        });
    };
    demoApp.setDefaultServicingScope = function () {
        JSONDataService.getData().then(function (response) {
            //alert(JSON.stringify(response));
            var servicing = response.data.bankdata[0].servicing;
            demoApp.RecoverableFee = servicing.defaultrecoverableFee;
            demoApp.PreservationFee = servicing.defaultpreservationFee;
            demoApp.LegalCost = servicing.defaultlegalCost;
            $scope.principal1 = 0;
            //alert(JSON.stringify(escrow));
        });
    };
    demoApp.setDefaultEscrowScope();
    demoApp.setDefaultServicingScope();
    demoApp.getBankBalance();
    demoApp.getEscrows();
    demoApp.getTransactions();
    demoApp.getServicings();


    demoApp.doEscrowTX = function () {
        JSONDataService.getData().then(function (response) {
            //alert(JSON.stringify(response));
            var escrow = response.data.bankdata[0].escrow;
            //alert(JSON.stringify(escrow));
            var bankTXInputData = new Object();
            bankTXInputData.emiVal = parseInt($scope.invamount);
            bankTXInputData.taxVal = escrow.defaultTax;
            bankTXInputData.insuranceVal = escrow.defaultInsurance;
            bankTXInputData.parcelId = escrow.defaultParcelId;
            bankTXInputData.invAccountNum = escrow.invAcNum;
            bankTXInputData.escrowParty = escrow.bankParty
            bankTXInputData.invParty = escrow.invParty;

            var url = `${apiBaseURL}create-escrow`;
            //console.log(JSON.stringify(bankTXInputData));
            //alert(JSON.stringify(bankTXInputData));
            $http.put(url, JSON.stringify(bankTXInputData)).then(
                   (result) => {
                       setAlertTimeout();
                       regcloseAlert();
                       //alert(result.data);
                       $("#result").html('<div class="alert alert-success"><button type="button" class="close">×</button>' + result.data + '</div>');
                       demoApp.getEscrows();
                       demoApp.getTransactions();
                       demoApp.getBankBalance();
                   },
                   (result) => {
                       setAlertTimeout();
                       regcloseAlert();
                       $("#result").html('<div class="alert alert-danger"><button type="button" class="close">×</button>' + result.data + '</div>');
                   }
               );
            $scope.invamount = '';
        });

    };
    var setAlertTimeout = function () {
        //timing the alert box to close after 5 seconds
        window.setTimeout(function () {
            $(".alert").fadeTo(500, 0).slideUp(500, function () {
                $(this).remove();
            });
        }, 3000);
    };
    var regcloseAlert = function () {
        //Adding a click event to the 'x' button to close immediately
        $('.alert .close').on("click", function (e) {
            alert("close");
            $(this).parent().fadeTo(500, 0).slideUp(500);
        });
    }

    demoApp.doServicingTX = function () {
        JSONDataService.getData().then(function (response) {
            //alert($scope.svcamount);
            var servicing = response.data.bankdata[0].servicing;
            var bankTXInputData = new Object();
            bankTXInputData.emiVal = parseInt($scope.svcamount);
            bankTXInputData.recoverableFeeValue = servicing.defaultrecoverableFee;
            bankTXInputData.preservationFeeValue = servicing.defaultpreservationFee;
            bankTXInputData.legalCostValue = servicing.defaultlegalCost;
            bankTXInputData.parcelId = servicing.defaultParcelId;
            bankTXInputData.invAccountNum = servicing.invAcNum;
            bankTXInputData.servicingParty = servicing.bankParty
            bankTXInputData.invParty = servicing.invParty;
            console.log(JSON.stringify(bankTXInputData));

            var url = `${apiBaseURL}create-servicing`;

            $http.put(url, JSON.stringify(bankTXInputData)).then(
                   (result) => {
                       setAlertTimeout();
                       regcloseAlert();
                       //alert(result.data);
                       $("#result").html('<div class="alert alert-success"><button type="button" class="close">×</button>' + result.data + '</div>');
                       demoApp.getServicings();
                       demoApp.getTransactions();
                       demoApp.getBankBalance();
                   },
                   (result) => {
                       setAlertTimeout();
                       regcloseAlert();
                       $("#result").html('<div class="alert alert-danger"><button type="button" class="close">×</button>' + result.data + '</div>');
                   }
               );
            $scope.svcamount = '';
        });
    };
    demoApp.logout = function () {
        $location.path('/signin');
    };

});

app.controller('LoginController', function ($location, $scope, $state, $window, $rootScope, LoginService) {
    const demoApp = this;
    var isAuthenticated = false;
    var fullName;
    demoApp.login = function () {
        LoginService.login(demoApp.username, demoApp.password).then(function (response) {
            angular.forEach(response.data.users[0].bank, function (value) {
                if (value.username == demoApp.username && value.password == demoApp.password) {
                    isAuthenticated = true;
                    fullName = value.fullname;
                }
            });
            if (isAuthenticated) {
                LoginService.setLoggedinUserName(fullName);
                $location.path('/dashboard');
                //$window.location.href = '/home.html';
            }
            else {
                $scope.error = "Incorrect username/password !";
            }

        });
    };
    });