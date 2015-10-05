"""
Script to convert jbovlaste XML (downloaded dump from jbovlaste)
to a Apache Solr readable XML format.
"""

import sys
import re
import xml.etree.ElementTree as ET
from collections import namedtuple

Valsi = namedtuple('Valsi', ['word', 'wtype', 'definition', 'notes', 'rafsi'])

DocRecord = namedtuple('DocRecord', ['id', 'jbo_text', 'eng_text', 'src'])


def iterate_valsi(jbovlaste_root_node):
    """
    Given the root node of XML Element Tree, iterates all the valsi namedtuples
    contained in the jbo->eng direction.

    Args:
        jbovlaste_root_node: XML Element pointing to the root 'dictionary' node

    Returns:
        iterator over Valsi namedtuples.
    """
    jbo_eng_direction = next(direction for direction in jbovlaste_root_node
                             if direction.attrib['from'] == 'lojban'
                             and direction.attrib['to'] == 'English')
    assert jbo_eng_direction is not None

    for valsi_node in jbo_eng_direction:
        word = valsi_node.attrib.get('word', '')
        wtype = valsi_node.attrib.get('type', '')
        definition = valsi_node.findtext('definition', default='')
        notes = valsi_node.findtext('notes', default='')
        rafsi = [rafsi_node.text for rafsi_node in valsi_node.findall('rafsi')]

        yield Valsi(word=word, wtype=wtype, definition=definition, notes=notes, rafsi=rafsi)


# convenience functions to generate opening, closing tag, and <a href=... tag
open_tag = lambda tagname: '&lt;%s&gt;' % tagname
close_tag = lambda tagname: '&lt;/%s&gt;' % tagname
a_tag = lambda href, text: '&lt;a href="%s"&gt;%s&lt;/a&gt;' % (href, text)


def replace_place_notations(text):
    """Replace place notations (e.g., $x_1$) with subscript notations (e.g., x<sub>1</sub>)"""
    text = re.sub(r'([a-z])_\{?(\d+)\}?',
                  '\\1%s\\2%s' % (open_tag('sub'), close_tag('sub')),
                  text)
    return re.sub(r'\$', '', text)


def sanitize_for_xml(text):
    """Sanitize some characters for Solr XML output."""
    return text.replace(u'&', '&amp;').replace(r'<', '&lt;').replace(r'>', '&gt;')


def replace_link_notations(text):
    """Replace link notations (e.g., {klama}) with anchor text (e.g., <a href=...)"""
    return re.sub(r'\{([\w\']+)\}',
                  a_tag('?q=\\1', '\\1'),
                  text)


def format_doc_record(valsi):
    """Given a valsi namedtuple, returns a formatted doc record for Solr index.

    Args:
        valsi: Valsi namedtuple

    Returns:
        DocRecord namedtuple (containing id, jbo_text, eng_text, src fields)
    """
    jbo_text = valsi.word

    eng_text_arr = []

    # Add word type
    eng_text_arr.extend([open_tag('b'), valsi.wtype, close_tag('b'), ' '])

    # Add rafsi
    if len(valsi.rafsi) > 0:
        eng_text_arr.extend(['rafsi: '])
        for individual_rafsi in valsi.rafsi:
            eng_text_arr.extend([' ', individual_rafsi, ' '])
        eng_text_arr.append(' ')

    # Add definition
    eng_text_arr.append(replace_place_notations(sanitize_for_xml(valsi.definition)))

    # Add notes
    if len(valsi.notes) > 0:
        notes = replace_place_notations(replace_link_notations(sanitize_for_xml(valsi.notes)))
        eng_text_arr.extend([' ', notes])

    src_url = ('http://jbovlaste.lojban.org/lookup.pl?'
               + 'Form=lookup.pl2&amp;Database=*&amp;Query=%s' % valsi.word)

    return DocRecord(id='jbovlaste:%s' % valsi.word,
                     jbo_text=jbo_text,
                     eng_text=''.join(eng_text_arr),
                     src=src_url)


def print_doc_records(records):
    """Print records as Solr XML"""

    print '<add>'
    for record in records:
        print '''<doc>
<field name="id">%s</field>
<field name="jbo_t">%s</field>
<field name="eng_t">%s</field>
<field name="src_t">%s</field>
</doc>''' % (record.id,
             record.jbo_text.encode('utf-8'),
             record.eng_text.encode('utf-8'),
             record.src)
    print '</add>'


def main():
    """Main function"""
    if len(sys.argv) == 1:
        print "Usage: python convert_jbovlaste.py [jbovlaste XML filename] > output XML filename"
        sys.exit(1)

    tree = ET.parse(sys.argv[1])
    root = tree.getroot()

    valsis = list(iterate_valsi(root))

    records = [format_doc_record(valsi) for valsi in valsis]

    print_doc_records(records)

if __name__ == '__main__':
    main()
