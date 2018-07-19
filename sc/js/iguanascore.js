
	var app = angular.module('sparql', ["chart.js"]);
app.directive('chart', function () {
    return {
        restrict: 'E',
        templateUrl: 'templates/chartTemplate.html',
        scope: {
            chartData: '='
        }
    };
});
	app.config(['ChartJsProvider', function (ChartJsProvider) {
    // Configure all charts
    ChartJsProvider.setOptions({
      colors : [ getRandomColor(), getRandomColor(), getRandomColor(), getRandomColor(), getRandomColor(), getRandomColor(), getRandomColor()],
      responsive: true,
      legend: {
            display: true
      }
    });
    // Configure all line charts
    ChartJsProvider.setOptions('line', {
      showLines: false
    });
  }]);
	Chart.defaults.global.colors=[ getRandomColor(), getRandomColor(), getRandomColor(), getRandomColor(), getRandomColor(), getRandomColor(), getRandomColor()];
	app.controller(
					"sparqlController",
					function($scope, $http, $location) {

					$scope.fileLoaded=false;
						$scope.results=[{title:"QMPH", dataset:"", id:"1", type:"chart-bar", labels:['a','b'], series: ['F','T','V'], data:[[1,1],[2,2],[2, 4]]},
{title:"QMPH_1", Visible:true, id:"img2",  type:"chart-line", labels:['c','d','e','f','g','h'], series: ['F','T','V'], data:[[5,5,5,5,5,5],[20,20,5,5,5,5],[12, 14,5,5,5,5]]}];
						$scope.endpointForm=[]
						$scope.compareUris=[];
						$scope.error = false;
						$scope.errormsg = '';
						$scope.store = null;

						$scope.choose=[{"con":{"value":"placeholder"}}];
						$scope.selectAction = function() {
    							console.log($scope.compareUris);
						};
						$scope.download = function(){
							html2canvas(document.querySelector('#img2')).then(canvas => {
    								var data = canvas.toDataURL();
                var docDefinition = {
			
                    content: ['Your Results', {
          			width: '*',
          			image: data,
        		},
				{
	        			table: {
	        				headerRows: 1,
	        				widths: [ 100,  100,  100, 100 ],

	        				body: [
						        [ 'First', 'Second', 'Third', 'The last one' ],
        		 	 			[ 'Value 1', 'Value 2', 'Value 3', 'Value 4' ],
          						[ 'bla', 'Val 2', 'Val 3', 'Val 4' ]
        					]
        				},
				}
		       
                    
    
                    ]
                };
                pdfMake.createPdf(docDefinition).download("Score_Details.pdf");
							});
            
 						};

						$scope.errorClear = function() {
							$scope.error = false;
							$scope.errormsg = '';
						};
						$scope.getQMPH2 = function($data){							
									var resultData = [];	
									console.log($data);
									for(result in $data.results.bindings){
										var dataset = result.dataset.toString().replace('http://iguana-benchmark.eu/recource/', '');
										var connection = result.connection.toString().replace('http://iguana-benchmark.eu/recource/', '')+" - "+task.toString().replace('http://iguana-benchmark.eu/recource/', '');
										var t = task.toString().substring(task.lastIndexOf('/'));
										//create qmph structure
										var taskDataset = {labels:[], data:[]};
										if($scope.results[t]!=null){
											if( $scope.results[t][dataset]!=null){
												taskDataset=$scope.results[t][dataset];
											}
										}
										else{
											$scope.results[t]={};
										}
										taskDataset.labels.push(connection);
										taskDataset.data.push(result.QMPH);
										$scope.results[t][dataset]=taskDataset;
										console.log($scope.results);							
									}
								};
						$scope.getQMPH =function(){
							//for taskUris : compareUris:
							for(task in $scope.compareUris) {
								//SELECT 
								$scope.sparqlSubmit("SELECT ?suiteID ?dataset ?connection (SUM(?qmph) AS ?QMPH) {?suiteID <http://www.w3.org/2000/01/rdf-schema#Class>  <http://iguana-benchmark.eu/class/Suite> . ?suiteID <http://iguana-benchmark.eu/properties/experiment> ?expID. ?expID <http://iguana-benchmark.eu/properties/dataset>  ?dataset . ?expID <http://iguana-benchmark.eu/properties/task> <"+task+">. <"+task+"> <http://iguana-benchmark.eu/properties/connection> ?connection. <"+task+"> ?p ?uuid . ?uuid <http://iguana-benchmark.eu/properties/queryMixes> ?qmph } GROUP BY ?suiteID ?dataset ?connection ", {'vars':["suiteID", "dataset","uuid","connection", "QMPH"]},
									$scope.getQMPH2);

							}
							
							
						}
						$scope.getNoQPH =function(){
							//for taskUris : compareUris:
							//SELECT 
							//add row 
						}
						$scope.getQPS =function(){
							//for taskUris : compareUris:
							//SELECT 
							//add row 
						}
						$scope.getEQE =function(){
							//for taskUris : compareUris:
							//SELECT 
							//add row 
						}
						$scope.getStats =function(){
							//for taskUris : compareUris:
							//SELECT 
							//add row 
						}
						$scope.getScore =function(){
							//for taskUris : compareUris:
							//SELECT 
							//add row 
						}
						$scope.retrieveResults = function(){
							$scope.getQPS();
							$scope.getEQE();
							$scope.getQMPH();
							$scope.getNoQPH();
							$scope.getStats();
							$scope.getScore();
							
							document.getElementById('results').hidden=false;
							document.getElementById('download').hidden=false;
							console.log($scope.compareUris);
 							location.hash = "results" ;

						};
						$scope.endpointF = function($data) {
							document.getElementById('choose').hidden=false;
							console.log($data.results.bindings);
							$scope.choose=$data.results.bindings;
							$scope.$apply(); 	
 							
							
 							location.hash = "choose" ;
							
						};
						$scope.sparqlSubmit = function($query, $vars, $callback) {
							$scope.error = false;
							document.getElementById('progress').hidden=false;
							if($scope.endpointForm.endpoint!=null && !($scope.endpointForm.endpoint=='')){
								console.log($scope.endpointForm.endpoint);
								$http({
									method : 'GET',
									
									url : $scope.endpointForm.endpoint+'?query='
													+ encodeURIComponent($query)
										})
										.then(
												function successCallback(response) {
													console.log(response.data);
													document.getElementById('progress').hidden=true;
													$callback(response.data);
												},
												function errorCallback(response) {
													$scope.error = true;
													$scope.errormsg = response.status
															+ ": "
															+ response.statusText;
													document.getElementById('progress').hidden=true;
										});
							
							}
							else{

								var f = document.getElementById('file').files[0],
        						r = new FileReader();

    							
    							r.onloadend = function(e) {
      								var data = e.target.result;
									rdfstore.create(function(err, store) {
											
											store.load("text/turtle", data, function(err){
													if(err){
														console.log(err);
													}
													$scope.store=store;
													store.execute($query, function(err, results) {
													var res = {"head": { $vars}, "results":{"bindings": results}}
													document.getElementById("progress").hidden="true";
													console.log(results);
													$callback(res);
													$scope.$apply(); 
									});
											});
									});
										

												
										
								};
    							

   								r.readAsBinaryString(f);

								
							};

						};
});

function getRandomColor() {
    var letters = '0123456789ABCDEF'.split('');
    var color = '#';
    for (var i = 0; i < 6; i++ ) {
        color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
}
