import unittest

from pyramid import testing


class ViewTests(unittest.TestCase):
    def setUp(self):
        self.config = testing.setUp()

    def tearDown(self):
        testing.tearDown()

    def test_home_view(self):
        from sisku2.views import home_view
        request = testing.DummyRequest()
        info = home_view(request)
        self.assertEqual(info['project'], 'sisku2')

    def test_search(self):
        from sisku2 import search
        self.assertRaises(AssertionError, lambda: search('klama', 0))

        # NOTE: these tests depend on the solr idex. Update when solr index is also updated.
        num_found, results = search('klama', 1)
        self.assertEqual(544, num_found)
        self.assertEqual('klama', results[0]['jbo_t'])
        self.assertEqual('I am coming.', results[0]['eng_t'])
        self.assertEqual('klama', results[1]['jbo_t'])
        self.assertEqual('http://jbovlaste.lojban.org/lookup.pl?Form=lookup.pl2&Database=*'
                         '&Query=klama', results[1]['src_t'])
