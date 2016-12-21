
#!/bin/bash
#
# see http://linuxcommand.org/lc3_adv_dialog.php
# xfce4-terminal -e "bash -c 'player.sh'"

PLAYER_CMD=(vlc)

SAVEIFS=$IFS
IFS=$(echo -en "\n\b")

# lire (player) ou marquer comme lu (mv)
showAction () {
    FILENAME=$1
    actionCmd=(dialog  --clear --cancel-label "Annuler" --output-fd 1 --title "Actions" --radiolist "Veuillez choisir une action" 10 35 8)

    actionOptions=(0 "Lire la vidéo" "ON" 1 "Marquer comme lu" "OFF")
    actionChoices=$("${actionCmd[@]}" "${actionOptions[@]}")

    if [[ ! -z "$actionChoices" ]]; then
        echo "actionChoices : ${actionChoices} of ${FILENAME}"
        if [ "$actionChoices" -eq 0 ] ; then
            # lire
            $("${PLAYER_CMD[@]}" "${FILENAME}")
        elif [ "$actionChoices" -eq 1 ] ; then
            # lu
            mkdir vus
            mv $FILENAME vus/
        fi
    fi
}

while [ 1 ]
do 
    i=0
    options=()
    for LINE in `ls -A1t`; do 
            if [ -f "$LINE" ]
            then
                options[i]=$i
                i=$((i+1))
                options[i]="$LINE"

                i=$((i+1))

                if [ $i == 2 ]
                        then
                           options[i]="ON"
                        else
                           options[i]="OFF"
                        fi
                i=$((i+1))
            fi
    done

    cmd=(dialog  --clear --cancel-label "Annuler" --output-fd 1 --title "'Vidéothèque'" --radiolist "'Veuillez choisir un fichier'" 30 55 30)
    choices=$("${cmd[@]}" "${options[@]}")
    
    if [[ -z "$choices" ]]; then
            break;
        else
            showAction ${options[${choices}+1]}
    fi		
done

clear
IFS=$SAVEIFS

