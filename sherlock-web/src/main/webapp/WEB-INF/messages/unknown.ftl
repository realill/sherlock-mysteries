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
I am sorry, I do not understand.
    <#break>
  <#case 1>
I'm not sure I understand.
    <#break>
  <#case 2>
I don't understand what you mean.
    <#break>
  <#default>
I am very sorry, but I didn't get that.
</#switch>

<#include "whatIsNext.ftl">
