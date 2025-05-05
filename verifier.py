import os
import subprocess
import shutil

INPUT_DIR = 'inputSSF'
VERIFIED_DIR = 'Verified'
VERIFY_SCRIPT = 'verify.py'
JAVA_CLASS = 'in.ud.convert.SSFtoCoNLLUConverter'
OUTPUT_FILE = 'output.txt'
SUPPORTED_SUFFIXES = ('-posn-name', '.pos.cnhk', '.dat', '.txt', '.mo.po', '.mo.pos.chnk')


def find_ssf_files(root):
    ssf_files = []
    for dirpath, _, filenames in os.walk(root):
        for filename in filenames:
            if filename.endswith(SUPPORTED_SUFFIXES):
                ssf_files.append(os.path.join(dirpath, filename))
    return ssf_files

def run_java_converter(input_path, output_path):
    cmd = (
        f'mvn exec:java '
        f'-Dexec.mainClass="{JAVA_CLASS}" '
        f'-Dexec.args="{input_path} {output_path}"'
    )
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.returncode == 0, result.stdout, result.stderr

def run_verifier(input_path, output_path):
    cmd = ['python', VERIFY_SCRIPT, input_path, output_path]
    result = subprocess.run(cmd)
    return result.returncode == 0

def ensure_dir(path):
    os.makedirs(path, exist_ok=True)

def move_to_verified(original_path):
    rel_path = os.path.relpath(original_path, INPUT_DIR)
    new_path = os.path.join(VERIFIED_DIR, rel_path)
    ensure_dir(os.path.dirname(new_path))
    shutil.move(original_path, new_path)
    print(f'✅ Verified and moved: {original_path} → {new_path}')

def main():
    ssf_files = find_ssf_files(INPUT_DIR)
    print(f'🔍 Found {len(ssf_files)} SSF files to process.')

    for ssf_file in ssf_files:
        print(f'🚧 Processing: {ssf_file}')
        success, out, err = run_java_converter(ssf_file, OUTPUT_FILE)

        if not success:
            print(f'❌ Java conversion failed for {ssf_file}\n{err}')
            continue

        if run_verifier(ssf_file, OUTPUT_FILE):
            move_to_verified(ssf_file)
        else:
            print(f'❌ Verification failed for {ssf_file}')

if __name__ == '__main__':
    main()