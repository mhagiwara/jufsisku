from pyramid.config import Configurator
from urllib2 import urlopen, URLError
import xml.etree.ElementTree as ET

RESULTS_PER_PAGE = 20


def _solrxml2results(response_body):
    """Internal method to convert solr's XML response string to a list of results.
    Arguments:
        response_body (str): XML string returned by solr
    Returns:
        tuple of (number of total results, list of results)
        where results is a dict with doc fields (e.g., id, jbo_t, ...) )and their values.
    """

    root = ET.fromstring(response_body)
    _, result_node = list(root)

    num_found = int(result_node.attrib['numFound'])

    results = []
    for doc in result_node:
        result = {}
        for field in doc:
            result[field.attrib['name']] = field.text
        results.append(result)

    return num_found, results


def search(query, page=1):
    """Issue a query to solr and returns the search results.

    Arguments:
        query (str): query string
        page (int):  1-base page number
    Returns:
        tuple of (number of search results found, list of search results)
    """
    assert page > 0
    start = (page - 1) * RESULTS_PER_PAGE
    url = ('http://localhost:8983/solr/select/?version=2.2&rows=%s&indent=on'
           '&q=jbo_t:%s+eng_t:%s&start=%s' % (RESULTS_PER_PAGE, query, query, start))

    try:
        url_response = urlopen(url)
        response_body = url_response.read()
        return _solrxml2results(response_body)

    except URLError:
        # return an empty list when an error occurs (most likely solr is not running)
        return (0, [])


def main(global_config, **settings):
    """This function returns a Pyramid WSGI application."""

    config = Configurator(settings=settings)
    config.include('pyramid_chameleon')
    config.add_static_view('static', 'static', cache_max_age=3600)
    config.add_route('home', '/')
    config.scan()
    return config.make_wsgi_app()
