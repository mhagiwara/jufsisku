import re
from pyramid.config import Configurator
from urllib2 import urlopen, URLError
import xml.etree.ElementTree as ET

RESULTS_PER_PAGE = 16


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
    query = query.replace(' ', '+')
    url = ('http://localhost:8983/solr/select/?version=2.2&rows=%s&indent=on'
           '&q=jbo_t:%s+eng_t:%s&start=%s' % (RESULTS_PER_PAGE, query, query, start))

    try:
        url_response = urlopen(url)
        response_body = url_response.read()
        return _solrxml2results(response_body)

    except URLError:
        # return an empty list when an error occurs (most likely solr is not running)
        return (0, [])


def highlight(results, query):
    """Given search results, returns a new list of results with query highlighted.

    Arguments:
        results: search results returned by search()
        query: query string

    Returns:
        a new list of serach results, with query highlighted by <span class="highlight">...<span>
    """

    def highlight_field(text):
        """Helper method to highlight words in query for a single field"""
        # Add dummy whitespace
        # This is necessary to avoid accidentally replacing substrings
        field = ' ' + text + ' '
        punc_re = r'[ \.,\?\!/\"\(\)\-]'
        for word in query.split(' '):
            field = re.sub(r'(%s)%s(%s)' % (punc_re, word, punc_re),
                           '\\1<span class="highlight">%s</span>\\2' % word,
                           field)
        return field[1:-1]

    new_results = []
    for result in results:
        new_result = dict(result)
        new_result['jbo_t'] = highlight_field(new_result['jbo_t'])
        new_result['eng_t'] = highlight_field(new_result['eng_t'])
        new_results.append(new_result)

    return new_results

def main(global_config, **settings):
    """This function returns a Pyramid WSGI application."""

    config = Configurator(settings=settings)
    config.include('pyramid_chameleon')
    config.add_static_view('static', 'static', cache_max_age=3600)
    config.add_route('home', '/')
    config.scan()
    return config.make_wsgi_app()
