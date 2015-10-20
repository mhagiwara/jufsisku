from webhelpers import paginate
from pyramid.view import view_config
from sisku2 import search, RESULTS_PER_PAGE


@view_config(route_name='home', renderer='templates/home.pt')
def home_view(request):
    if 'q' not in request.params:
        # Home page
        return {'home': True,
                'query': ''}
    else:
        # Search page
        query = request.params['q']
        current_page = int(request.params.get('p', 1))
        num_found, results = search(query, page=current_page)

        # Generate pager
        page = paginate.Page(range(num_found),
                             page=current_page, items_per_page=RESULTS_PER_PAGE,
                             url=lambda page: '/jufsisku/?q=%s&p=%s' % (query, page))
        pager_html = page.pager(format='$link_first $link_previous ~3~ $link_next $link_last')

        return {'home': False,
                'num_found': num_found,
                'results': results,
                'query': query,
                'pager': pager_html}
