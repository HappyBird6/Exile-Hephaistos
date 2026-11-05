"""Parse source Augment cards without selecting item IDs or guessing game rules."""
from html.parser import HTMLParser
from pathlib import Path
import json

class Node:
    def __init__(self, tag='', attrs=()):
        self.tag = tag
        self.attrs = dict(attrs)
        self.children = []
    def nodes(self):
        for child in self.children:
            if isinstance(child, Node):
                yield child
                yield from child.nodes()
    def text(self):
        return ''.join(child.text() if isinstance(child, Node) else child for child in self.children)

class Parser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root = Node()
        self.stack = [self.root]
    def handle_starttag(self, tag, attrs):
        node = Node(tag, attrs)
        self.stack[-1].children.append(node)
        if tag == 'br':
            node.children.append('\n')
        if tag not in {'img','br','hr','meta','link','input','source','wbr','area','base','embed','param','col'}:
            self.stack.append(node)
    def handle_endtag(self, tag):
        for index in range(len(self.stack) - 1, 0, -1):
            if self.stack[index].tag == tag:
                self.stack = self.stack[:index]
                break
    def handle_data(self, text):
        self.stack[-1].children.append(text)

def cards(path):
    parser = Parser()
    parser.feed(Path(path).read_text(encoding='utf-8'))
    result = {}
    for node in parser.root.nodes():
        if node.tag != 'div' or node.attrs.get('class') != 'd-flex border-top rounded':
            continue
        descendants = list(node.nodes())
        link = next((n for n in descendants if n.tag == 'a' and 'SoulCore' in n.attrs.get('class', '')), None)
        if link is None:
            continue
        slug = link.attrs['href']
        label = next(n for n in descendants if n.tag == 'a' and n.attrs.get('href') == slug and n.text().strip())
        record = {
            'id': slug,
            'name': label.text().strip(),
            'properties': [n.text().strip() for n in descendants if n.attrs.get('class') in ('property', 'requirements')],
            'ordinary': [n.text().strip() for n in descendants if n.attrs.get('class') == 'implicitMod'],
            'bonded': [n.text().strip() for n in descendants if n.attrs.get('class') == 'bondedMod'],
            'socketBound': any('Socket-bound' == n.text().strip() for n in descendants),
            'image': next(n.attrs['src'] for n in descendants if n.tag == 'img'),
        }
        if slug in result and result[slug] != record:
            raise ValueError('Conflicting duplicate source card: ' + slug)
        result[slug] = record
    return result

if __name__ == '__main__':
    directory = Path(__file__).resolve().parents[1] / 'docs/evidence/0.5.5/augment-expansion-20261105'
    for locale in ['us','kr','cn','tw','jp','sp']:
        parsed = cards(directory / ('Augment.' + locale + '.html'))
        (directory / ('cards.' + locale + '.json')).write_bytes((json.dumps(list(parsed.values()), ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
        print(locale, len(parsed))
