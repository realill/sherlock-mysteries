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
You have finished the your case with reward of ${tool.money(stats.score)} points.
<#if stats.correctAnswers == 0>
You were not able to answer any of the case questions.
<#elseif stats.correctAnswers == stats.totalAnswers>
All of ${stats.totalAnswers} questions were answered correctly.
<#else>
 You have answered ${stats.correctAnswers} out of ${stats.totalAnswers} questions correctly.
</#if>

Please provide your feedback about Sherlock Mysteries in Assistant Directory.
