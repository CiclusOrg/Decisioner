<!DOCTYPE html>
<html lang="en">
	<head>
		<meta name="layout" content="main"/>
		<title>SustenAgro - Admin</title>
	</head>
	<body>
		<div class="row main">
			<div class="col-sm-10 col-sm-offset-1 content">
				<h5 class="text-primary page-header">DSL Code</h5>				
				<form id="dsl_form" action="/sustenagro/admin/dslCreate" method="post">
					<div class="form-group">
						<textarea id="code" name="code" class="form-control"></textarea>
					</div>
					<input type="submit" class="btn btn-primary" value="generate" />
				</form>
				
				<h5 class="text-primary page-header">Result</h5>
				<div id='result'>
					
				</div>
				
				<script type="application/javascript">
					
					var fd = new FormData();    
					fd.append( 'code', $("#code").val() );
					
					$( "#dsl_form" ).submit(function( event ) {
					  $.post(
					  	$("#dsl_form").attr('action'),
					  	{'code':  $("#code").val() },
					  	function( data ) {
						  $('#result').html(data);
						}
					  );
					  event.preventDefault();
					});
				</script>
				
			</div>
		</div>
	</body>
</html>