from pyramid.view import view_config


@view_config(route_name='home', renderer='templates/mytemplate.pt')
def my_view(request):
    if 'q' not in request.params:
        # Home page
        return {'project': 'sisku2'}
    else:
        # Search page
        return {'project': 'query = %s' % request.params['q']}
