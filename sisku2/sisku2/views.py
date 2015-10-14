from pyramid.view import view_config
from sisku2 import search

@view_config(route_name='home', renderer='templates/home.pt')
def home_view(request):
    if 'q' not in request.params:
        # Home page
        return {'home': True}
    else:
        # Search page
        query = request.params['q']
        num_found, results = search(query)
        return {'home': False,
                'num_found': num_found}
