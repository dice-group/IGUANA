<form:form commandName="insertUser" method="POST" action="insertJdbcContact.do" id="userdetailsid" >		
 
			<fieldset>
				<legend>User details</legend>
				<ol>
					<li>
						<label for=name>Name</label>
						<form:input path="name"  type="text" placeholder="First and last name" />
					</li>
					<li>
						<label for=name>Date</label>
					<form:input path="dob" type="date" required="true" />
					</li>
					<li>
						<label for=email>Email</label>
						<form:input path="email" type="text" required="true" />
					</li>
					<li>
						<label for=phone>Phone</label>
						<form:input path="phone" type="text" required="true" />
					</li>
				</ol>
			</fieldset>
			<fieldset>
				<legend>User address</legend>
				<ol>
					<li>
 
						<label for=address>Address</label>
						<form:input path="address" type="text" required="true" />
					</li>
					<li>
						<label for=postcode>Post code</label>
						<form:input path="pincode" type="text" required="true" />
					</li>
					<li>
						<label for=country>Country</label>
						<form:input path="country" type="text" required="true" />
					</li>
				</ol>
			</fieldset>
			<fieldset>
				<button type=submit>Save User Details!</button>
			</fieldset>
		</form:form>