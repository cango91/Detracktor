#!/usr/bin/env python3
"""
Version Code Calculation Test Script

This script tests different versioning strategies to find the optimal solution for Android version codes
that satisfies all constraints:

1. Stable releases must end in 0
2. Pre-releases must be ordered: alpha < beta < rc < stable
3. Within same type, higher numbers must have higher codes
4. Must be compatible with existing published version: v1.1.0-alpha.1 = 10100989
5. Must generate higher codes for future releases

Constraints from user:
- v1.0.0-alpha.1 -> A
- v1.0.0-alpha.2 -> B  
- v1.0.0-beta.1 -> C
- v1.0.0-beta.2 -> D
- v1.0.0-rc.1 -> E
- v1.0.0-rc.2 -> F
- v1.0.0-xxxx.99 -> G
- v1.0.0 -> H

Required ordering: H > F > E > D > C > B > A > G
AND H must end in 0
"""

import re
from typing import Tuple, Optional

def parse_version(version: str) -> Tuple[int, int, int, Optional[str], Optional[int]]:
    """Parse a version string into components."""
    # Remove 'v' prefix if present
    version = version.lstrip('v')
    
    # Regex for semver with pre-release
    match = re.match(r'^(\d+)\.(\d+)\.(\d+)(?:-(.+))?$', version)
    if not match:
        raise ValueError(f"Invalid version format: {version}")
    
    major, minor, patch = int(match.group(1)), int(match.group(2)), int(match.group(3))
    pre_release = match.group(4)
    
    pre_type = None
    pre_number = None
    
    if pre_release:
        # Extract pre-release type and number
        pre_match = re.match(r'^([a-zA-Z]+)\.?(\d+)?$', pre_release)
        if pre_match:
            pre_type = pre_match.group(1)
            pre_number = int(pre_match.group(2)) if pre_match.group(2) else 1
        else:
            pre_type = pre_release
            pre_number = 1
    
    return major, minor, patch, pre_type, pre_number

def calculate_version_code_strategy1(version: str) -> int:
    """
    Strategy 1: Reserve trailing 0 for stable, use negative offsets for pre-releases
    
    Base = MAJOR * 10000000 + MINOR * 100000 + PATCH * 1000
    Stable: Base + 0 (ends in 0)
    Pre-releases: Base - (TYPE_OFFSET - NUMBER)
    """
    major, minor, patch, pre_type, pre_number = parse_version(version)
    base = major * 10000000 + minor * 100000 + patch * 1000
    
    if not pre_type:  # Stable release
        return base  # Ends in 0
    
    # Pre-release offsets (higher offset = lower priority)
    type_offsets = {
        'alpha': 500,
        'beta': 400, 
        'rc': 300,
    }
    
    offset = type_offsets.get(pre_type, 600)  # Unknown types get lowest priority
    return base - (offset - pre_number)

def calculate_version_code_strategy2(version: str) -> int:
    """
    Strategy 2: Use last 3 digits for pre-release info, reserve x000 for stable
    
    Base = MAJOR * 10000000 + MINOR * 100000 + PATCH * 1000
    Stable: Base (ends in 000)
    Pre-releases: Base + (TYPE_CODE * 100 + NUMBER)
    """
    major, minor, patch, pre_type, pre_number = parse_version(version)
    base = major * 10000000 + minor * 100000 + patch * 1000
    
    if not pre_type:  # Stable release
        return base  # Ends in 000
    
    # Type codes (lower = lower priority)
    type_codes = {
        'alpha': 1,
        'beta': 2,
        'rc': 3,
    }
    
    type_code = type_codes.get(pre_type, 0)  # Unknown types get lowest
    return base + (type_code * 100 + min(pre_number, 99))

def calculate_version_code_strategy3(version: str, compatibility_offset: int = 0) -> int:
    """
    Strategy 3: Advanced system with compatibility offset
    
    Base = MAJOR * 10000000 + MINOR * 100000 + PATCH * 1000
    Stable: Base + 900 + compatibility_offset (to ensure it ends in 0, we adjust)
    Pre-releases: Base + TYPE_OFFSET + NUMBER + compatibility_offset
    """
    major, minor, patch, pre_type, pre_number = parse_version(version)
    base = major * 10000000 + minor * 100000 + patch * 1000
    
    if not pre_type:  # Stable release
        # We want it to end in 0, so we calculate backwards
        # If base ends in 000, we add 0
        # If base ends in other, we adjust to make it end in 0
        stable_code = base + compatibility_offset
        # Ensure it ends in 0
        last_digit = stable_code % 10
        if last_digit != 0:
            stable_code += (10 - last_digit)
        return stable_code
    
    # Pre-release type offsets
    type_offsets = {
        'alpha': 100,
        'beta': 200,
        'rc': 300,
    }
    
    type_offset = type_offsets.get(pre_type, 50)  # Unknown types get lowest
    return base + type_offset + min(pre_number, 99) + compatibility_offset

def calculate_version_code_strategy4(version: str, compatibility_offset: int = 0) -> int:
    """
    Strategy 4: Reverse ordering with proper stable release handling
    
    Base = MAJOR * 10000000 + MINOR * 100000 + PATCH * 1000
    Stable: Base + 900 + compatibility_offset (ends in 0)
    Pre-releases: Base + (900 - TYPE_PENALTY + NUMBER) + compatibility_offset
    
    This ensures stable > rc > beta > alpha > unknown
    """
    major, minor, patch, pre_type, pre_number = parse_version(version)
    base = major * 10000000 + minor * 100000 + patch * 1000
    
    if not pre_type:  # Stable release
        # Stable gets the highest value and ends in 0
        stable_code = base + 900 + compatibility_offset
        # Ensure it ends in 0
        last_digit = stable_code % 10
        if last_digit != 0:
            stable_code += (10 - last_digit)
        return stable_code
    
    # Pre-release type penalties (higher penalty = lower priority)
    type_penalties = {
        'rc': 100,      # rc.1 = base + 900 - 100 + 1 = base + 801
        'beta': 200,    # beta.1 = base + 900 - 200 + 1 = base + 701  
        'alpha': 300,   # alpha.1 = base + 900 - 300 + 1 = base + 601
    }
    
    penalty = type_penalties.get(pre_type, 400)  # Unknown gets highest penalty
    return base + (900 - penalty + min(pre_number, 99)) + compatibility_offset

def calculate_version_code_strategy5(version: str, compatibility_offset: int = 0) -> int:
    """
    Strategy 5: Ultimate solution with proper ordering and trailing zeros
    
    Base = MAJOR * 10000000 + MINOR * 100000 + PATCH * 1000
    
    For stable releases: Base + compatibility_offset (ensure ends in 0)
    For pre-releases: Base - (1000 - TYPE_PRIORITY * 100 - NUMBER) + compatibility_offset
    
    This ensures:
    - Stable releases end in 0 and are highest
    - Pre-releases are ordered correctly: alpha < beta < rc < stable
    - Higher numbers within same type get higher codes
    """
    major, minor, patch, pre_type, pre_number = parse_version(version)
    base = major * 10000000 + minor * 100000 + patch * 1000
    
    if not pre_type:  # Stable release
        stable_code = base + compatibility_offset
        # Ensure it ends in 0
        last_digit = stable_code % 10
        if last_digit != 0:
            stable_code += (10 - last_digit)
        return stable_code
    
    # Pre-release type priorities (higher priority = higher code)
    type_priorities = {
        'alpha': 1,     # alpha.1 = base - (1000 - 100 - 1) = base - 899
        'beta': 2,      # beta.1 = base - (1000 - 200 - 1) = base - 799
        'rc': 3,        # rc.1 = base - (1000 - 300 - 1) = base - 699
    }
    
    priority = type_priorities.get(pre_type, 0)  # Unknown gets lowest priority
    pre_release_offset = 1000 - (priority * 100) - min(pre_number, 99)
    return base - pre_release_offset + compatibility_offset

def test_strategy(strategy_func, strategy_name: str, compatibility_offset: int = 0):
    """Test a versioning strategy with sample data."""
    print(f"\n=== Testing {strategy_name} ===")
    if compatibility_offset > 0:
        print(f"Compatibility offset: {compatibility_offset}")
    
    test_versions = [
        "v1.0.0-alpha.1",    # A
        "v1.0.0-alpha.2",    # B
        "v1.0.0-beta.1",     # C
        "v1.0.0-beta.2",     # D
        "v1.0.0-rc.1",       # E
        "v1.0.0-rc.2",       # F
        "v1.0.0-unknown.99", # G
        "v1.0.0",            # H
        # Additional test cases
        "v1.1.0-alpha.1",    # The problematic existing version
        "v1.1.0-alpha.2",    # What we need to be higher
        "v1.1.0",            # Future stable
    ]
    
    results = []
    for version in test_versions:
        if compatibility_offset > 0:
            code = strategy_func(version, compatibility_offset)
        else:
            code = strategy_func(version)
        results.append((version, code))
        ends_in_zero = str(code).endswith('0')
        print(f"{version:20} -> {code:10} {'✓' if ends_in_zero and 'alpha' not in version and 'beta' not in version and 'rc' not in version and 'unknown' not in version else ''}")
    
    # Check ordering constraints for v1.0.0 series
    codes = {v.split('-')[0] + ('-' + v.split('-')[1] if '-' in v else ''): c for v, c in results[:8]}
    
    A = codes["v1.0.0-alpha.1"]
    B = codes["v1.0.0-alpha.2"] 
    C = codes["v1.0.0-beta.1"]
    D = codes["v1.0.0-beta.2"]
    E = codes["v1.0.0-rc.1"]
    F = codes["v1.0.0-rc.2"]
    G = codes["v1.0.0-unknown.99"]
    H = codes["v1.0.0"]
    
    # Check required ordering: H > F > E > D > C > B > A > G
    ordering_correct = H > F > E > D > C > B > A > G
    stable_ends_in_zero = str(H).endswith('0')
    
    print(f"\nOrdering check (H > F > E > D > C > B > A > G): {'✓' if ordering_correct else '✗'}")
    print(f"Stable ends in 0: {'✓' if stable_ends_in_zero else '✗'}")
    
    # Check compatibility with existing version
    existing_alpha1 = next((c for v, c in results if v == "v1.1.0-alpha.1"), None)
    new_alpha2 = next((c for v, c in results if v == "v1.1.0-alpha.2"), None)
    
    if existing_alpha1 and new_alpha2:
        compatibility_ok = new_alpha2 > 10100989  # Must be higher than published version
        print(f"v1.1.0-alpha.2 ({new_alpha2}) > 10100989: {'✓' if compatibility_ok else '✗'}")
        
        return ordering_correct and stable_ends_in_zero and compatibility_ok
    
    return ordering_correct and stable_ends_in_zero

def find_optimal_compatibility_offset(strategy_func=calculate_version_code_strategy3):
    """Find the minimum compatibility offset needed."""
    print(f"\n=== Finding Optimal Compatibility Offset for {strategy_func.__name__} ===")
    
    # We need v1.1.0-alpha.2 to be > 10100989
    # Let's test different offsets
    for offset in range(0, 50000, 1000):
        try:
            code = strategy_func("v1.1.0-alpha.2", offset)
            if code > 10100989:
                print(f"Minimum offset needed: {offset}")
                print(f"v1.1.0-alpha.2 would be: {code}")
                return offset
        except:
            continue
    
    return 0

def main():
    """Run all tests and find the optimal solution."""
    print("Android Version Code Calculation Test")
    print("=" * 50)
    
    # Test all strategies
    print("\nTesting different strategies...")
    
    strategy1_ok = test_strategy(calculate_version_code_strategy1, "Strategy 1: Negative Offsets")
    strategy2_ok = test_strategy(calculate_version_code_strategy2, "Strategy 2: Last 3 Digits")
    
    # Find optimal offset for strategy 3
    optimal_offset3 = find_optimal_compatibility_offset(calculate_version_code_strategy3)
    strategy3_ok = test_strategy(calculate_version_code_strategy3, "Strategy 3: With Compatibility Offset", optimal_offset3)
    
    # Find optimal offset for strategy 4
    optimal_offset4 = find_optimal_compatibility_offset(calculate_version_code_strategy4)
    strategy4_ok = test_strategy(calculate_version_code_strategy4, "Strategy 4: Reverse Ordering", optimal_offset4)
    
    # Find optimal offset for strategy 5
    optimal_offset5 = find_optimal_compatibility_offset(calculate_version_code_strategy5)
    strategy5_ok = test_strategy(calculate_version_code_strategy5, "Strategy 5: Ultimate Solution", optimal_offset5)
    
    print(f"\n=== SUMMARY ===")
    print(f"Strategy 1 (Negative Offsets): {'✓ PASS' if strategy1_ok else '✗ FAIL'}")
    print(f"Strategy 2 (Last 3 Digits): {'✓ PASS' if strategy2_ok else '✗ FAIL'}")
    print(f"Strategy 3 (Compatibility Offset): {'✓ PASS' if strategy3_ok else '✗ FAIL'}")
    print(f"Strategy 4 (Reverse Ordering): {'✓ PASS' if strategy4_ok else '✗ FAIL'}")
    print(f"Strategy 5 (Ultimate Solution): {'✓ PASS' if strategy5_ok else '✗ FAIL'}")
    
    if strategy5_ok:
        print(f"\n🎉 RECOMMENDED SOLUTION: Strategy 5 with offset {optimal_offset5}")
        print("This strategy satisfies all constraints and handles the compatibility issue.")
    elif strategy4_ok:
        print(f"\n🎉 ALTERNATIVE: Strategy 4 with offset {optimal_offset4}")
        print("This strategy satisfies all constraints and handles the compatibility issue.")
    elif strategy3_ok:
        print(f"\n🎉 ALTERNATIVE: Strategy 3 with offset {optimal_offset3}")
        print("This strategy satisfies all constraints and handles the compatibility issue.")
    elif strategy1_ok:
        print(f"\n⚠️  Strategy 1 works but doesn't handle compatibility")
    else:
        print(f"\n❌ Need to design a new strategy!")

if __name__ == "__main__":
    main()
