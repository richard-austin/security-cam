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

I have detected a change of Virgin Media broadband IP address, this is now ${current_ip}	

Thanks

Raspberry pi
EOT

curl --ssl-reqd \
  --url 'smtps://smtp.virginmedia.com:465' \
  --user 'rdaustin@virginmedia.com:DC10plus' \
  --mail-from 'rdaustin@virginmedia.com' \
  --mail-rcpt 'richard.david.austin@gmail.com' \
  --upload-file /home/www-data/changed_ip_email.txt


fi

echo $current_ip > /home/www-data/myip
