(function () {
    window.items = [];

    String.prototype.toDOM = function () {
        var d = document,
            i, a = d.createElement("div"),
            b = d.createDocumentFragment();
        a.innerHTML = this;
        while (i = a.firstChild) b.appendChild(i);
        return b;
    };

    function render() {
        var vFilter = document.getElementById("rechercher").value;
        var itemsFiltered = vFilter ? items.filter(function (item) {
            item.body = item.body ||  '';
            return (item.title.toLowerCase().indexOf(vFilter.toLowerCase()) !== -1) ||
                (item.body.toLowerCase().indexOf(vFilter.toLowerCase()) !== -1)
        }) : window.items;

        document.getElementById("items").innerHTML = '';
        if (itemsFiltered.length === 0) {
            var el = `
<div class="empty">Aucun résultat</div>
`;
            document.getElementById("items").appendChild(el.toDOM());
        }

        for (var index in itemsFiltered) {
            var item = itemsFiltered[index];
            var ext = item.tags && item.tags.length ? item.tags[0] : '';
            var jsonTitle = JSON.stringify(item.title);
            var extCls = ext ? '': 'hide';
            var cls = '',
                style = '';
            if (item.download === -1) {
                cls = 'error';
            } else if (item.download === 1) {
                cls = 'complete';
            } else {
                style = 'background: linear-gradient(to right, #5bc0de ' + (item.download * 100) + '%, white '+(item.download * 100)+'%, white ' + (100 - (item.download * 100)) + '%); */';
            }
            var el = `
<article class="card ${cls}" >
  <h3>
    ${item.label} ${item.download == -1 ? "" : ' : '+Math.ceil(item.download * 100)+'%'}<span class="label ${extCls}">${ext}</span>
    
    <button class='error shyButton cancel' onclick="cancel(event, this)" type="button" data-title='${jsonTitle}'>Annuler</button>
</h3>
        <div class="bar" style="${style}"></div>
</article>
`;
            document.getElementById("items").appendChild(el.toDOM());
        }
    }

    document.getElementById('rechercher').addEventListener('keyup', render);
    document.getElementById('actualiser').addEventListener('click', loadData);

    window.cancel = function(event, me){
        var title = JSON.parse(me.getAttribute('data-title'));
        document.getElementById('loader').className = ''; 
        fetch('/data/files/'+encodeURI(title), {
            method : 'DELETE'
        })
        .then(function (response) {
            document.getElementById('loader').className = 'hide';
    
            if (response.status !== 200) {
                throw "oups";
            }
        })
        .catch(function (e) {
            alert('Erreur lors de l\'annulation');
        });
   }
    
    function loadData() {
        document.getElementById('loader').className = '';
        fetch('/data/files')
        .then(function (response) {
            if (response.status !== 200) {
                throw "oups";
            } else {
                return response.json();
            }
        })
        .then(function (data) {
            window.items = data;
            render();
            document.getElementById('loader').className = 'hide';
        })
        .catch(function (e) {
            document.getElementById('loader').className = 'hide';
            alert('Erreur lors de la récupération des fichiers');
        });
    }
    
    loadData();
}());