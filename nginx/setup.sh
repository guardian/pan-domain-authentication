DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
nginxHome=`nginx -V 2>&1 | grep "configure arguments:" | sed 's/[^*]*conf-path=\([^ ]*\)\/nginx\.conf.*/\1/g'`

sudo ln -fs $DIR/panDomainExample.conf $nginxHome/sites-enabled/panDomainExample.conf
sudo ln -fs $DIR/panDomainExample.crt $nginxHome/panDomainExample.crt
sudo ln -fs $DIR/panDomainExample.key $nginxHome/panDomainExample.key
sudo nginx -s stop
sudo nginx