var app = angular.module('invReportingAppModule', ['ui.router', 'ui.bootstrap', 'ngLoadingOverlay']);

// Fix for unhandled rejections bug.
app.config(['$stateProvider', '$urlRouterProvider', function ($stateProvider, $urlRouterProvider) {
    //$urlRouterProvider.otherwise('/signin');

    $stateProvider
      .state('signin', {
          url: '/signin',
          templateUrl: '/web/investor/Login.html'
      })
      .state('dashboard', {
          url: '/dashboard',
          templateUrl: '/web/investor/Home.html'
      });
}]);
app.controller("DefController", function ($location) {
    $location.path('/signin')
});
app.factory('JSONDataService', ['$http', function ($http) {
    var getData = function () {
        return $http.get('/web/investor/Data/metadata.json');

    }
    return {
        getData: getData
    };
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
    const apiBaseURL = "http://localhost:10010/api/invreporting/"; // TODO remove harcoded urls
    let peers = [];

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

    demoApp.getInvestors = () => $http.get(apiBaseURL + "investor-all")
           .then((response) => demoApp.myInvestors = Object.keys(response.data)
               .map((key) => response.data[key].state.data)
               .reverse());

    demoApp.getBankBalance = () => $http.get(apiBaseURL + "bank-balance")
           .then((result) => {
               //alert(JSON.stringify(result.data.state.data));
               demoApp.balance = result.data.bankBalanceValue;
           });

    demoApp.getInvestorBalance = () => $http.get(apiBaseURL + "investor-balance")
                .then((result) => {
                    //alert(JSON.stringify(result.data.state.data));
                    demoApp.investorBalance = result.data.investorBalanceValue;
                });

    demoApp.getTransactions = () => $http.get(apiBaseURL + "transactions")
        .then((result) => {
            demoApp.trans = result.data;
            //alert(JSON.stringify(demoApp.trans));
        });
    demoApp.getBankBalance();
    demoApp.getInvestorBalance();
    demoApp.getEscrows();
    demoApp.getTransactions();
    demoApp.getInvestors();


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
            angular.forEach(response.data.users[0].investor, function (value) {
                if (value.username == demoApp.username && value.password == demoApp.password) {
                    isAuthenticated = true;
                    fullName = value.fullname;
                }
            });
            if (isAuthenticated) {
                LoginService.setLoggedinUserName(fullName);
                $location.path('/dashboard');
            }
            else {
                $scope.error = "Incorrect username/password !";
            }

        });
    };
});