"use strict";

const app = angular.module('invReportingAppModule', ['ui.bootstrap', 'ngLoadingOverlay']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('InvReportingAppController', function ($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "http://localhost:10007/api/invreporting/"; // TODO remove harcoded urls
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
                alert(JSON.stringify(demoApp.trans));
            });
        demoApp.getBankBalance();
        demoApp.getInvestorBalance();
        demoApp.getEscrows();
        demoApp.getTransactions();
        demoApp.getInvestors();

    demoApp.doEscrowTX = function () {

    // TODO: read all this from JSON

        var defaultTax = 10;
        var defaultInsurance = 10;
        var defaultParcelId = "e0adbf57-eb1e-4455-956e-f30a4f43006c";
        var invAcNum = "5098674354";
        var bankParty = "O=BankPartyA,L=London,C=GB";
        var invParty = "O=InvestorPartyB,L=Bangalore,C=IN";

        var bankTXInputData = new Object();
        bankTXInputData.emiVal = 300; // TODO: get it from form field
        bankTXInputData.taxVal = defaultTax;
        bankTXInputData.insuranceVal = defaultInsurance;
        bankTXInputData.parcelId = defaultParcelId;
        bankTXInputData.invAccountNum = invAcNum;
        bankTXInputData.escrowParty = bankParty
        bankTXInputData.invParty = invParty;

        //alert(JSON.stringify(bankTXInputData));
        var url = `${apiBaseURL}create-escrow`;

        $http.put(url, JSON.stringify(bankTXInputData)).then(
               (result) => {
                  alert(result.data);
                   demoApp.getEscrows();
               },
               (result) => {
                   modalInstance.displayMessage(result.data);
               }
           );

    };

    demoApp.doServicingTX = function () {

        // TODO: read all this from JSON

            var defaultrecoverableFee = 10;
            var defaultpreservationFee = 10;
            var defaultlegalCost = 10;
            var defaultParcelId = "e0adbf57-eb1e-4455-956e-f30a4f43006c";
            var invAcNum = "5098674354";
            var bankParty = "O=BankPartyA,L=London,C=GB";
            var invParty = "O=InvestorPartyB,L=Bangalore,C=IN";

            var bankTXInputData = new Object();
            bankTXInputData.emiVal = 300; // TODO: get it from form field
            bankTXInputData.recoverableFeeValue = defaultrecoverableFee;
            bankTXInputData.preservationFeeValue = defaultpreservationFee;
            bankTXInputData.legalCostValue = defaultlegalCost;
            bankTXInputData.parcelId = defaultParcelId;
            bankTXInputData.invAccountNum = invAcNum;
            bankTXInputData.servicingParty = bankParty
            bankTXInputData.invParty = invParty;

            //alert(JSON.stringify(bankTXInputData));
            var url = `${apiBaseURL}create-servicing`;

            $http.put(url, JSON.stringify(bankTXInputData)).then(
                   (result) => {
                      alert(result.data);
                       demoApp.getServicings();
                   },
                   (result) => {
                       modalInstance.displayMessage(result.data);
                   }
               );

        };
});