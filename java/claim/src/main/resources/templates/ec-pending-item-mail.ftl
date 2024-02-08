<html>
<head>
<style>
    table, th, td {
      border: 1px solid black;
      border-collapse: collapse;
    }

    th, td {
      padding: 10px;
    }
</style>
</head>

<body>
    <p><H3>Hi Council Admin</H3></p>

    <p>Pending action item for ${candidate.entityName}. Following candidate has applied for certificate.</p>

    <p>
    <table>
      <caption><h2>Candidate Details</h2></caption>

        <tr>
            <td>Name</td>
            <td>${candidate.name}</td>
        </tr>
        <tr>
            <td>Email</td>
            <td>${candidate.email}</td>
        </tr>
        <tr>
            <td>Council</td>
            <td>${candidate.council}</td>
        </tr>
        <tr>
            <td>Nursing Collage</td>
            <td>${candidate.nursingCollage}</td>
        </tr>
        <tr>
            <td>Country</td>
            <td>${candidate.country}</td>
        </tr>
        <tr>
            <td>State</td>
            <td>${candidate.state}</td>
        </tr>

        <#if verificationLink?has_content>
            <tr>
                <td>Verification Link</td>
                <td><a href = "${verificationLink}">Verify Candidate</a></td>
            </tr>
        </#if>
    </table>
    </p>

    <div>Please follow up to respected entity</div>

    <p>Thank you,</p>
    <p>&#60; Registration Credential Issuing Authority &#62;</p>
</body>
</html>
