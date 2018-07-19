	var app = angular.module('choose', []);
	app.controller(
					"ChooseController",
					function($scope, $http) {
						$scope.data = [];
						$scope.time = 0.0;
						$scope.error = false;
						$scope.errormsg = '';
						$scope.errorClear = function() {
							$scope.error = false;
							$scope.errormsg = '';
						};
						$scope.sparqlSubmit = function($query, $result) {
							$scope.error = false;
							$http({
								method : 'GET',
								url : $scope.endpointForm.endpoint+'?query='
												+ encodeURIComponent($query)
									})
									.then(
											function successCallback(response) {
												console.log(response.data);
												$result = response.data;
											},
											function errorCallback(response) {
												$scope.error = true;
												$scope.errormsg = response.status
														+ ": "
														+ response.statusText;
											});
						};
					});
