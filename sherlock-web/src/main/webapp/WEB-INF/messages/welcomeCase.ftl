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
<#switch tool.random(4)>
  <#case 0>
    What do you want to do next?
    <#break>
  <#case 1>
	What are we doing now?
    <#break>
  <#case 2>
    Hello again, what's next?
    <#break>
  <#default>
    What are we doing next?
</#switch>