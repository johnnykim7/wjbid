envget(){ docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$1" | grep -E "^$2=" | head -1 | cut -d= -f2-; }
if [ "$WHO" = N ]; then C=wjbid-notification-bp-notification-1; DB=notification_db; else C=bidding-agency-bidding-agency-1; DB=bidding_agency; fi
U=$(envget $C DB_USERNAME); P=$(envget $C DB_PASSWORD)
