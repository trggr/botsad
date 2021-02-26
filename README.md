Via Advanced Search get all gardens. At the bottom of the page there's a link to CSV file

https://tools.bgci.org/garden_advanced_search.php?action=Find&mode=&ftrCountry=All&ftrInstitutionType=All&ftrKeyword=&x=84&y=22#results

https://tools.bgci.org/garden_advanced_search.php?action=Find&mode=&ftrCountry=All&ftrInstitutionType=All&ftrKeyword=&x=84&y=22&export=1

View page source to see Garden IDs. 

	Example:  ID=1024 
	"garden.php?id=1024&amp;ftrCountry=All&amp;ftrKeyword=&amp;ftrBGCImem=&amp;ftrIAReg=">Myall Park Botanic Garden

Edit page to arrive at list of garden IDs. There are total 3693 institutions:

        1024 Myall Park Botanic Garden
        1412 Hunter Region Botanic Gardens
        107 Royal Tasmanian Botanical Gardens
        ...


The information about each can be obtained via request:

        https://tools.bgci.org/garden.php?id=1024

For example, use 
	curl 

The information about the garden is between tags <article> ... </article>

<div class="breadcrumb">BGCI > Garden</div>









