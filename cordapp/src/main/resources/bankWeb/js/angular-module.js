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

     demoApp.getBankBalance = () => $http.get(apiBaseURL + "bank-balance")
            .then((result) => {
                //alert(JSON.stringify(result.data.state.data));
                demoApp.balance = result.data.bankBalanceValue;
            });

        demoApp.getTransactions = () => $http.get(apiBaseURL + "transactions")
            .then((result) => {
                demoApp.trans = result.data;
                alert(JSON.stringify(demoApp.trans));
            });
        demoApp.getBankBalance();
        demoApp.getEscrows();
        demoApp.getTransactions();

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

});