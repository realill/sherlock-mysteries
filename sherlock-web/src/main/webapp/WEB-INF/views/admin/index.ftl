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
<#import "/admin/reusable.ftl" as p>
<!doctype html>
<html lang="en">
  <head>
<@p.includes />
    <title>Sherlock Mysteries Admin</title>
  </head>
  <body>
    <nav class="navbar navbar-dark fixed-top bg-dark flex-md-nowrap p-0 shadow">
      <a class="navbar-brand col-sm-3 col-md-2 mr-0" href="/admin/">Sherlock Mysteries Admin</a>
    </nav>

    <div class="container-fluid">
      <div class="row">
<@p.navigation />

        <main role="main" class="col-md-9 ml-sm-auto col-lg-10 px-4">
          <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
            <h1 class="h2">Cases</h1> 
	          <div class="btn-group mr-2">
	          	<a class="btn btn-sm btn-outline-secondary" href="./update-case">New Case</a>
	          </div>
          </div>
          <table class="table table-sm">
              <thead>
                <th>Case Id</th>
                <th>Case Name</th>
                <th>Alternative Names</th>
                <th>Category</th>
                <th>Case Data Id</th>
                <th>Enabled</th>
              </tr>
              </thead>
              <tbody>
              <#if cases??>
               <#list cases as case>
                <tr>
                  <td><a href="./update-case?caseId=${case.caseId}"> ${(case.caseId)!}</a></td>
                  <td>${(case.name)!}</td>
                  <td>${(case.alternativeNames?join(","))!}</td>
                  <td>${(case.category)!}</td>
                  <td>${(case.caseDataId)!}</td>
                  <td>${case.enabled?c}</td>
                </tr>
               </#list>
              </#if>
            </tbody>
          </table>
        </main>
      </div>
    </div>
  </body>
</html>