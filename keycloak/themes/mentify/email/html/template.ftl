<#macro emailLayout>
<!doctype html>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="color-scheme" content="light">
  <title>Mentify</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f6fb;font-family:Arial,Helvetica,sans-serif;color:#172033;-webkit-text-size-adjust:100%;">
  <div style="display:none;font-size:1px;color:#f4f6fb;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;">Your Mentify account is ready. Complete the secure action inside.</div>
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f6fb;">
    <tr>
      <td align="center" style="padding:36px 16px;">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;">
          <tr>
            <td style="padding:0 0 18px 4px;">
              <span style="font-size:21px;line-height:28px;font-weight:700;letter-spacing:-0.4px;color:#273b91;">mentify</span>
              <span style="display:block;padding-top:3px;font-size:11px;line-height:16px;letter-spacing:1.6px;text-transform:uppercase;color:#78829a;">Learn with confidence</span>
            </td>
          </tr>
          <tr>
            <td style="background-color:#ffffff;border:1px solid #e7eaf2;border-radius:14px;overflow:hidden;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                <tr><td height="5" style="height:5px;background-color:#4f46e5;font-size:0;line-height:0;">&nbsp;</td></tr>
                <tr>
                  <td style="padding:36px 40px 38px;">
                    <#nested>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="padding:20px 12px 0;text-align:center;font-size:12px;line-height:19px;color:#7a8498;">
              This is an automated security email from Mentify.<br>
              Please do not reply to this message.
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
</#macro>
