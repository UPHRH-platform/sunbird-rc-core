<html>
<head>
<style>
    table {
      border-collapse: collapse;
    }

    th, td {
      text-align: left;
      padding: 8px;
    }

    tr:nth-child(odd) {
      background-color: #D6EEEE;
    }

    #cand {
      font-family: Arial, Helvetica, sans-serif;
      border-collapse: collapse;
    }

</style>
</head>

<body>
<table>
    <caption><h2>Candidate Details</h2></caption>
    <tr>
    	<td>Student Name</td>

    	<#if candidate.name?has_content>
          <td>${candidate.name}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Registration Number</td>

    	<#if candidate.registrationNumber?has_content>
          <td>${candidate.registrationNumber}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Reference No</td>

    	<#if candidate.refNo?has_content>
          <td>${candidate.refNo}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Phone Number</td>

    	<#if candidate.phoneNumber?has_content>
          <td>${candidate.phoneNumber}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Fathers Name</td>

    	<#if candidate.fathersName?has_content>
          <td>${candidate.fathersName}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Email</td>

    	<#if candidate.email?has_content>
          <td>${candidate.email}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Date</td>

    	<#if candidate.date?has_content>
          <td>${candidate.date}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Council</td>

    	<#if candidate.council?has_content>
          <td>${candidate.council}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Professional Qualification</td>

    	<#if candidate.professionalQualification?has_content>
          <td>${candidate.professionalQualification}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Training Center</td>

    	<#if candidate.trainingCenter?has_content>
          <td>${candidate.trainingCenter}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>WorkPlace</td>

    	<#if candidate.workPlace?has_content>
          <td>${candidate.workPlace}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Payment Status</td>

    	<#if candidate.paymentStatus?has_content>
          <td>${candidate.paymentStatus}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Claim Type</td>

    	<#if candidate.claimType?has_content>
          <td>${candidate.claimType}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>State</td>

    	<#if candidate.state?has_content>
          <td>${candidate.state}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>District</td>

    	<#if candidate.district?has_content>
          <td>${candidate.district}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
    <tr>
    	<td>Country</td>

    	<#if candidate.country?has_content>
          <td>${candidate.country}</td>
        <#else>
          <td> </td>
        </#if>
    </tr>
</table>
<div style="width:300px;background:#eee;height:30px;">
    <table id="cand" class="tb" style="width:300px;">
        <tr>
        <td>
            <input type=button style="height: 40px; width: 90px;"
            onClick="location.href='${claimUrl}/api/v1/outsideStudent/verify/${entityId}/APPROVED'"
             value='Approve'>
        </td>

        <td>
            <input type=button style="height: 40px; width: 90px;"
            onClick="location.href='${claimUrl}/api/v1/outsideStudent/verify/${entityId}/REJECTED'"
             value='Reject'>
        </td>
        </tr>
    </table>
</div>
</body>
</html>
