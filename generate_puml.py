import os
import re

def parse_java_file(filepath):
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()

    # Extract module name
    module_match = re.search(r'(.*?)/src/(?:main|test)/java', filepath)
    if module_match:
        module = os.path.basename(module_match.group(1))
    else:
        module = 'Unknown'

    # Extract package name
    pkg_match = re.search(r'package\s+([\w\.]+);', content)
    pkg = pkg_match.group(1) if pkg_match else 'default'

    # Remove comments to avoid false matches
    content = re.sub(r'//.*', '', content)
    content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)

    # Extract class/interface declaration
    class_match = re.search(r'(?:public|private|protected)?\s*(?:abstract)?\s*(class|interface|enum)\s+(\w+)(?:\s+extends\s+([\w<>,\s]+))?(?:\s+implements\s+([\w<>,\s]+))?\s*\{', content)
    
    if not class_match:
        return None

    type_, name, extends, implements = class_match.groups()
    
    # Extract fields for associations
    fields = re.findall(r'(?:private|protected|public)?\s+(?:final\s+|static\s+)*([A-Z]\w*(?:<.*?>)?)\s+\w+\s*(?:=.*?)?;', content)

    # Clean extends/implements
    def clean_types(s):
        if not s: return []
        s = re.sub(r'<.*?>', '', s)
        return [t.strip() for t in s.split(',')]

    return {
        'module': module,
        'pkg': pkg,
        'type': type_,
        'name': name,
        'extends': clean_types(extends),
        'implements': clean_types(implements),
        'fields': list(set(clean_types(','.join(fields))))
    }

def main():
    base_dir = '/home/zebastor/Documents/mis/sw2/p2p'
    classes = {}
    
    # Find all java files
    for root, dirs, files in os.walk(base_dir):
        for f in files:
            if f.endswith('.java'):
                info = parse_java_file(os.path.join(root, f))
                if info:
                    classes[info['name']] = info

    # Generate PlantUML
    puml = []
    puml.append('@startuml')
    puml.append('left to right direction')
    puml.append('skinparam packageStyle rectangle')
    puml.append('skinparam componentStyle uml2')
    puml.append('')
    
    # Group by module -> package
    modules = {}
    for name, info in classes.items():
        mod = info['module']
        pkg = info['pkg']
        if mod not in modules:
            modules[mod] = {}
        if pkg not in modules[mod]:
            modules[mod][pkg] = []
        modules[mod][pkg].append(info)
        
    for mod, pkgs in modules.items():
        puml.append(f'component "{mod}" {{')
        for pkg, pk_classes in pkgs.items():
            if pkg != 'default':
                puml.append(f'  package "{pkg}" {{')
            for info in pk_classes:
                type_ = info['type']
                name = info['name']
                indent = '    ' if pkg != 'default' else '  '
                if type_ == 'class':
                    puml.append(f'{indent}class {name}')
                elif type_ == 'interface':
                    puml.append(f'{indent}interface {name}')
                elif type_ == 'enum':
                    puml.append(f'{indent}enum {name}')
            if pkg != 'default':
                puml.append('  }')
        puml.append('}')
    
    puml.append('')
    
    # Relationships
    for name, info in classes.items():
        for ext in info['extends']:
            if ext in classes:
                puml.append(f'{ext} <|-- {name}')
        for impl in info['implements']:
            if impl in classes:
                puml.append(f'{impl} <|.. {name}')
        for field in info['fields']:
            if field in classes and field != name:
                puml.append(f'{name} --> {field}')

    puml.append('@enduml')
    
    with open('diagrama_clases_modulos.puml', 'w', encoding='utf-8') as f:
        f.write('\n'.join(puml))
    print("PlantUML generated at diagrama_clases_modulos.puml")

if __name__ == '__main__':
    main()
