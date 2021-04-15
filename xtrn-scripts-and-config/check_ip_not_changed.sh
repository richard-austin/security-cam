read -r last_ip < /home/www-data/myip

echo "Last IP = ${last_ip}"

current_ip=`curl -s 'https://api.ipify.org/?format=json' | python3 -c "import sys, json; print(json.load(sys.stdin)['ip'])"`
echo "Current IP = ${current_ip}"

if [ $current_ip != $last_ip ]; then
	cat <<EOT > /home/www-data/changed_ip_email.txt
From: "Raspberry pi" <rdaustin@virginmedia.com>
To: "Richard Austin" richard.david.austin@gmail.com>
Subject: Change of public IP address

Hi Richard,

I have detected a change of Virgin Media broadband IP address, this is now ${current_ip}.

Please go to the web application at the new address and use the "Save Current Public IP" option  on the General menu to stop these emails continuing to be sent.

Thanks

Raspberry pi
EOT

# Send the email
curl --ssl-reqd \
  --url 'smtps://smtp.virginmedia.com:465' \
  --user 'rdaustin@virginmedia.com:DC10plus' \
  --mail-from 'rdaustin@virginmedia.com' \
  --mail-rcpt 'richard.david.austin@gmail.com' \
  --upload-file /home/www-data/changed_ip_email.txt
fi

# The myip file will now be updated with an API call. That way notifications will continue to
#  be sent until the user responds on the new IP address. As IP changes are mst likely to be accompanied by
#  some sort of outage, this method is more likely to get an email through than sending just one close to an outage
#  then resetting straight away.
# echo $current_ip > /home/www-data/myip
