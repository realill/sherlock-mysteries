<#--
  Copyright 2020 Google LLC

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#macro includes>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css" integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO" crossorigin="anonymous">
	
    <link href="/static/admin/admin.css" rel="stylesheet">
</#macro>

<#macro navigation>
        <nav class="col-md-2 d-none d-md-block bg-light sidebar">
          <div class="sidebar-sticky">
            <ul class="nav flex-column">
              <li class="nav-item">
                <a class="nav-link" href="/admin/config">
                  Configuration
                </a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/admin/">
                  Cases
                </a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/admin/case-data">
                  Cases Data
                </a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/admin/dialogflow">
                  Dialogflow
                </a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/admin/import-log">
                  Import Log
                </a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/admin/generate-directory">
                  Generate Directory
                </a>
              </li>
            </ul>    
          </div>
        </nav>
</#macro>

<#macro errorsAndMessages>
          <#if message??>
	          <div class="alert alert-success" role="alert">
	           <pre>${message}</pre> 
	  		  </div>
  		  </#if>
          <#if errors?? && errors?size != 0>
	          <div class="alert alert-danger" role="alert">
	  			<#list errors as e>
	  				${e}<br/>
	  			</#list>
	  		  </div>
  		  </#if>
</#macro>